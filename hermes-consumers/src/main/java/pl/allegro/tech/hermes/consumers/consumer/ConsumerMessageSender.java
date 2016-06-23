package pl.allegro.tech.hermes.consumers.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.timer.ConsumerLatencyTimer;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.rate.InflightsPool;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResultLogInfo;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;

import java.time.Duration;
import java.util.Objects;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;

public class ConsumerMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerMessageSender.class);
    private final ScheduledExecutorService retrySingleThreadExecutor;
    private final ExecutorService deliveryReportingExecutor;
    private final SuccessHandler successHandler;
    private final ErrorHandler errorHandler;
    private final ConsumerRateLimiter rateLimiter;
    private final MessageSenderFactory messageSenderFactory;
    private final InflightsPool inflight;
    private final FutureAsyncTimeout<MessageSendingResult> async;
    private final int asyncTimeoutMs;
    private int requestTimeoutMs;
    private ConsumerLatencyTimer consumerLatencyTimer;
    private MessageSender messageSender;
    private Subscription subscription;

    private volatile boolean running = true;

    public ConsumerMessageSender(Subscription subscription, MessageSenderFactory messageSenderFactory, SuccessHandler successHandler,
                                 ErrorHandler errorHandler, ConsumerRateLimiter rateLimiter, ExecutorService deliveryReportingExecutor,
                                 InflightsPool inflight, HermesMetrics hermesMetrics, int asyncTimeoutMs,
                                 FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeout) {
        this.deliveryReportingExecutor = deliveryReportingExecutor;
        this.successHandler = successHandler;
        this.errorHandler = errorHandler;
        this.rateLimiter = rateLimiter;
        this.messageSenderFactory = messageSenderFactory;
        this.messageSender = messageSenderFactory.create(subscription);
        this.subscription = subscription;
        this.inflight = inflight;
        this.retrySingleThreadExecutor = Executors.newScheduledThreadPool(1);
        this.async = futureAsyncTimeout;
        this.requestTimeoutMs = subscription.getSerialSubscriptionPolicy().getRequestTimeout();
        this.asyncTimeoutMs = asyncTimeoutMs;
        this.consumerLatencyTimer = hermesMetrics.latencyTimer(subscription);
    }

    public void shutdown() {
        running = false;
    }


    public void sendAsync(Message message) {
        sendAsync(message, 0);
    }

    private void sendAsync(Message message, int delayMillis) {
        retrySingleThreadExecutor.schedule(() -> sendMessage(message), delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Method is calling MessageSender and is registering listeners to handle response.
     * Main responsibility of this method is that no message will be fully processed or rejected without release on semaphore.
     */
    public void sendMessage(final Message message) {
        rateLimiter.acquire();
        ConsumerLatencyTimer.Context timer = consumerLatencyTimer.time();
        CompletableFuture<MessageSendingResult> response = async.within(messageSender.send(message), Duration.ofMillis(asyncTimeoutMs + requestTimeoutMs));
        response.thenAcceptAsync(new ResponseHandlingListener(message, timer), deliveryReportingExecutor);
    }

    public synchronized void updateSubscription(Subscription newSubscription) {
        boolean endpointUpdated = !this.subscription.getEndpoint().equals(newSubscription.getEndpoint());
        boolean subscriptionPolicyUpdated = !Objects.equals(this.subscription.getSerialSubscriptionPolicy(), newSubscription.getSerialSubscriptionPolicy());
        this.requestTimeoutMs = newSubscription.getSerialSubscriptionPolicy().getRequestTimeout();
        this.subscription = newSubscription;
        if (endpointUpdated || subscriptionPolicyUpdated) {
            this.messageSender = messageSenderFactory.create(newSubscription);
        }
    }

    private boolean willExceedTtl(Message message, long delay) {
        long ttl = TimeUnit.SECONDS.toMillis(subscription.getSerialSubscriptionPolicy().getMessageTtl());
        long remainingTtl = Math.max(ttl - delay, 0);
        return message.isTtlExceeded(remainingTtl);
    }

    private void handleFailedSending(Message message, MessageSendingResult result) {
        if (shouldReduceSendingRate(result)) {
            rateLimiter.registerFailedSending();
        } else {
            rateLimiter.registerSuccessfulSending();
        }
        errorHandler.handleFailed(message, subscription, result);
    }

    private void handleMessageDiscarding(Message message, MessageSendingResult result) {
        inflight.release();
        errorHandler.handleDiscarded(message, subscription, result);
    }

    private void handleMessageSendingSuccess(Message message, MessageSendingResult result) {
        inflight.release();
        successHandler.handle(message, subscription, result);
    }

    private boolean shouldReduceSendingRate(MessageSendingResult result) {
        return !result.isRetryLater() && shouldResendMessage(result);
    }

    private boolean messageSentSucceeded(MessageSendingResult result) {
        return result.succeeded() || (result.isClientError() && !subscription.getSerialSubscriptionPolicy().isRetryClientErrors());
    }

    private boolean shouldResendMessage(MessageSendingResult result) {
        return !result.succeeded() && (!result.isClientError() || subscription.getSerialSubscriptionPolicy().isRetryClientErrors());
    }

    class ResponseHandlingListener implements java.util.function.Consumer<MessageSendingResult> {

        private final Message message;
        private final ConsumerLatencyTimer.Context timer;

        public ResponseHandlingListener(Message message, ConsumerLatencyTimer.Context timer) {
            this.message = message;
            this.timer = timer;
        }

        @Override
        public void accept(MessageSendingResult result) {
            timer.stop();
            if (result.succeeded()) {
                rateLimiter.registerSuccessfulSending();
                handleMessageSendingSuccess(message, result);
            } else {
                handleFailedSending(message, result);

                List<String> succeededUris = result.getSucceededUris(ConsumerMessageSender.this::messageSentSucceeded);
                message.incrementRetryCounter(succeededUris);

                long retryDelay = extractRetryDelay(result);
                if (running && shouldAttemptResending(result, retryDelay)) {
                    retrySingleThreadExecutor.schedule(() -> retrySending(result), retryDelay, TimeUnit.MILLISECONDS);
                } else {
                    handleMessageDiscarding(message, result);
                }
            }
        }

        private boolean shouldAttemptResending(MessageSendingResult result, long retryDelay) {
            return !willExceedTtl(message, retryDelay) && shouldResendMessage(result);
        }

        private long extractRetryDelay(MessageSendingResult result) {
            long defaultBackoff = subscription.getSerialSubscriptionPolicy().getMessageBackoff();
            long ttl = TimeUnit.SECONDS.toMillis(subscription.getSerialSubscriptionPolicy().getMessageTtl());
            return result.getRetryAfterMillis().map(delay -> Math.min(delay, ttl)).orElse(defaultBackoff);
        }

        private void retrySending(MessageSendingResult result) {
            if (result.isLoggable()) {
                result.getLogInfo().stream().forEach(this::logResultInfo);
            }

            sendMessage(message);
        }

        private void logResultInfo(MessageSendingResultLogInfo logInfo) {
            logger.debug(
                    format("Retrying message send to endpoint %s; messageId %s; offset: %s; partition: %s; sub id: %s; rootCause: %s",
                            logInfo.getUrl(), message.getId(), message.getOffset(), message.getPartition(),
                            subscription.getId(), logInfo.getRootCause()),
                    logInfo.getFailure());
        }
    }
}
