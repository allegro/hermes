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
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;

import java.time.Duration;
import java.util.Objects;
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

    private volatile boolean consumerIsConsuming = true;

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
        consumerIsConsuming = false;
    }

    /**
     * Method is calling MessageSender and is registering listeners to handle response.
     * Main responsibility of this method is that no message will be fully processed or rejected without release on semaphore.
     */
    public void sendMessage(final Message message) {
        while (consumerIsConsuming) {
            try {
                submitAsyncSendMessageRequest(message, consumerLatencyTimer);
                return;
            } catch (RuntimeException e) {
                handleFailedSending(message, failedResult(e));
                if (!consumerIsConsuming || isTtlExceeded(message)) {
                    handleMessageDiscarding(message, failedResult(e));
                    return;
                }
            }
        }
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

    private void submitAsyncSendMessageRequest(final Message message, final ConsumerLatencyTimer consumerLatencyTimer) {
        rateLimiter.acquire();
        ConsumerLatencyTimer.Context timer = consumerLatencyTimer.time();
        CompletableFuture<MessageSendingResult> response = async.within(messageSender.send(message), Duration.ofMillis(asyncTimeoutMs + requestTimeoutMs));
        response.thenAcceptAsync(new ResponseHandlingListener(message, timer), deliveryReportingExecutor);
    }

    private boolean isTtlExceeded(Message message) {
        return willExceedTtl(message, 0);
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
        return !result.isRetryLater() && subscriptionAllowsResending(result);
    }

    private boolean subscriptionAllowsResending(MessageSendingResult result) {
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
                if (result.isUnauthorized()) {
                    message.incrementNumberOfUnauthorizedRequests();
                }

                if (!(result.isUnauthorized() && message.getRetryCounter() == 0 && message.getNumberOfUnauthorizedRequests() == 1)) {
                    handleFailedSending(message, result);
                    message.incrementRetryCounter();

                    long retryDelay = extractRetryDelay(result);
                    if (consumerIsConsuming && shouldAttemptResending(result, retryDelay)) {
                        retrySingleThreadExecutor.schedule(() -> retrySending(result), retryDelay, TimeUnit.MILLISECONDS);
                    } else {
                        handleMessageDiscarding(message, result);
                    }
                } else {
                    sendMessage(message);
                }
            }
        }

        private boolean shouldAttemptResending(MessageSendingResult result, long retryDelay) {
            return !willExceedTtl(message, retryDelay) && subscriptionAllowsResending(result);
        }

        private long extractRetryDelay(MessageSendingResult result) {
            long defaultBackoff = subscription.getSerialSubscriptionPolicy().getMessageBackoff();
            long ttl = TimeUnit.SECONDS.toMillis(subscription.getSerialSubscriptionPolicy().getMessageTtl());
            return result.getRetryAfterMillis().map(delay -> Math.min(delay, ttl)).orElse(defaultBackoff);
        }

        private void retrySending(MessageSendingResult result) {
            if (result.isLoggable()) {
                logger.info(
                        format("Retrying message send to endpoint %s; messageId %s; offset: %s; partition: %s; sub id: %s; rootCause: %s",
                                subscription.getEndpoint(), message.getId(), message.getOffset(), message.getPartition(),
                                subscription.getId(), result.getRootCause()),
                        result.getFailure());
            }
            sendMessage(message);
        }
    }
}
