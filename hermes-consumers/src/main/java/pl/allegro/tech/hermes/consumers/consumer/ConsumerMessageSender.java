package pl.allegro.tech.hermes.consumers.consumer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.timer.ConsumerLatencyTimer;
import pl.allegro.tech.hermes.consumers.consumer.rate.InflightsPool;
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResultLogInfo;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

public class ConsumerMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerMessageSender.class);
    private final ExecutorService deliveryReportingExecutor;
    private final List<SuccessHandler> successHandlers;
    private final List<ErrorHandler> errorHandlers;
    private final SerialConsumerRateLimiter rateLimiter;
    private final MessageSenderFactory messageSenderFactory;
    private final Clock clock;
    private final InflightsPool inflight;
    private final FutureAsyncTimeout<MessageSendingResult> async;
    private final int asyncTimeoutMs;
    private final HermesMetrics hermesMetrics;

    private int requestTimeoutMs;
    private ConsumerLatencyTimer consumerLatencyTimer;
    private MessageSender messageSender;
    private Subscription subscription;

    private ScheduledExecutorService retrySingleThreadExecutor;
    private volatile boolean running = true;

    public ConsumerMessageSender(Subscription subscription,
                                 MessageSenderFactory messageSenderFactory,
                                 List<SuccessHandler> successHandlers,
                                 List<ErrorHandler> errorHandlers,
                                 SerialConsumerRateLimiter rateLimiter,
                                 ExecutorService deliveryReportingExecutor,
                                 InflightsPool inflight,
                                 HermesMetrics hermesMetrics,
                                 int asyncTimeoutMs,
                                 FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeout,
                                 Clock clock) {
        this.deliveryReportingExecutor = deliveryReportingExecutor;
        this.successHandlers = successHandlers;
        this.errorHandlers = errorHandlers;
        this.rateLimiter = rateLimiter;
        this.messageSenderFactory = messageSenderFactory;
        this.clock = clock;
        this.messageSender = messageSenderFactory.create(subscription);
        this.subscription = subscription;
        this.inflight = inflight;
        this.async = futureAsyncTimeout;
        this.requestTimeoutMs = subscription.getSerialSubscriptionPolicy().getRequestTimeout();
        this.asyncTimeoutMs = asyncTimeoutMs;
        this.hermesMetrics = hermesMetrics;
        this.consumerLatencyTimer = hermesMetrics.latencyTimer(subscription);
    }

    public void initialize() {
        running = true;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(subscription.getQualifiedName() + "-retry-executor-%d").build();
        this.retrySingleThreadExecutor = Executors.newScheduledThreadPool(1, threadFactory);
    }

    public void shutdown() {
        running = false;
        retrySingleThreadExecutor.shutdownNow();
        try {
            retrySingleThreadExecutor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.warn("Failed to stop retry executor within one minute with following exception", e);
        }
    }

    public void sendAsync(Message message) {
        sendAsync(message, calculateMessageDelay(message.getPublishingTimestamp()));
    }

    private void sendAsync(Message message, int delayMillis) {
        retrySingleThreadExecutor.schedule(() -> sendMessage(message), delayMillis, TimeUnit.MILLISECONDS);
    }

    private int calculateMessageDelay(long publishingMessageTimestamp) {
        Integer delay = subscription.getSerialSubscriptionPolicy().getSendingDelay();
        if (INTEGER_ZERO.equals(delay)) {
            return delay;
        }

        long messageAgeAtThisPoint = clock.millis() - publishingMessageTimestamp;

        delay = delay - (int) messageAgeAtThisPoint;

        return Math.max(delay, INTEGER_ZERO);
    }

    /**
     * Method is calling MessageSender and is registering listeners to handle response.
     * Main responsibility of this method is that no message will be fully processed or rejected without release on semaphore.
     */
    private void sendMessage(final Message message) {
        rateLimiter.acquire();
        ConsumerLatencyTimer.Context timer = consumerLatencyTimer.time();
        CompletableFuture<MessageSendingResult> response = async.within(
                messageSender.send(message),
                Duration.ofMillis(asyncTimeoutMs + requestTimeoutMs)
        );
        response.thenAcceptAsync(new ResponseHandlingListener(message, timer), deliveryReportingExecutor)
                .exceptionally(e -> {
                    logger.error("An error occurred while handling message sending response of subscription {} [partition={}, offset={}, id={}]",
                            subscription.getQualifiedName(), message.getPartition(), message.getOffset(), message.getId(), e);
                    return null;
                });
    }

    public void updateSubscription(Subscription newSubscription) {
        boolean endpointUpdated = !this.subscription.getEndpoint().equals(newSubscription.getEndpoint());
        boolean subscriptionPolicyUpdated = !Objects.equals(
                this.subscription.getSerialSubscriptionPolicy(),
                newSubscription.getSerialSubscriptionPolicy()
        );
        boolean endpointAddressResolverMetadataChanged = !Objects.equals(
                this.subscription.getEndpointAddressResolverMetadata(),
                newSubscription.getEndpointAddressResolverMetadata()
        );
        boolean oAuthPolicyChanged = !Objects.equals(
                this.subscription.getOAuthPolicy(), newSubscription.getOAuthPolicy()
        );

        this.subscription = newSubscription;
        this.requestTimeoutMs = newSubscription.getSerialSubscriptionPolicy().getRequestTimeout();

        boolean httpClientChanged = this.subscription.isHttp2Enabled() != newSubscription.isHttp2Enabled();

        if (endpointUpdated || subscriptionPolicyUpdated || endpointAddressResolverMetadataChanged
                || oAuthPolicyChanged || httpClientChanged) {
            this.messageSender = messageSenderFactory.create(newSubscription);
        }
    }

    private boolean willExceedTtl(Message message, long delay) {
        long ttl = TimeUnit.SECONDS.toMillis(subscription.getSerialSubscriptionPolicy().getMessageTtl());
        long remainingTtl = Math.max(ttl - delay, 0);
        return message.isTtlExceeded(remainingTtl);
    }

    private void handleFailedSending(Message message, MessageSendingResult result) {
        registerResultInRateLimiter(result);
        retrySending(message, result);
        errorHandlers.forEach(h -> h.handleFailed(message, subscription, result));
    }

    private void registerResultInRateLimiter(MessageSendingResult result) {
        if (result.ignoreInRateCalculation(subscription.getSerialSubscriptionPolicy().isRetryClientErrors(),
                subscription.hasOAuthPolicy())) {
            rateLimiter.registerSuccessfulSending();
        } else {
            rateLimiter.registerFailedSending();
        }
    }

    private void retrySending(Message message, MessageSendingResult result) {
        List<URI> succeededUris = result.getSucceededUris(ConsumerMessageSender.this::messageSentSucceeded);
        message.incrementRetryCounter(succeededUris);

        long retryDelay = extractRetryDelay(message, result);
        if (shouldAttemptResending(message, result, retryDelay)) {
            retrySingleThreadExecutor.schedule(() -> resend(message, result), retryDelay, TimeUnit.MILLISECONDS);
        } else {
            handleMessageDiscarding(message, result);
        }
    }

    private boolean shouldAttemptResending(Message message, MessageSendingResult result, long retryDelay) {
        return !willExceedTtl(message, retryDelay) && shouldResendMessage(result);
    }

    private long extractRetryDelay(Message message, MessageSendingResult result) {
        long defaultBackoff = message.updateAndGetCurrentMessageBackoff(subscription.getSerialSubscriptionPolicy());
        long ttl = TimeUnit.SECONDS.toMillis(subscription.getSerialSubscriptionPolicy().getMessageTtl());
        return result.getRetryAfterMillis().map(delay -> Math.min(delay, ttl)).orElse(defaultBackoff);
    }

    private void resend(Message message, MessageSendingResult result) {
        if (result.isLoggable()) {
            result.getLogInfo().forEach(logInfo -> logResultInfo(message, logInfo));
        }
        sendMessage(message);
    }

    private void logResultInfo(Message message, MessageSendingResultLogInfo logInfo) {
        logger.debug(
                format("Retrying message send to endpoint %s; messageId %s; offset: %s; partition: %s; sub id: %s; rootCause: %s",
                        logInfo.getUrlString(), message.getId(), message.getOffset(), message.getPartition(),
                        subscription.getQualifiedName(), logInfo.getRootCause()),
                logInfo.getFailure());
    }

    private void handleMessageDiscarding(Message message, MessageSendingResult result) {
        inflight.release();
        errorHandlers.forEach(h -> h.handleDiscarded(message, subscription, result));
    }

    private void handleMessageSendingSuccess(Message message, MessageSendingResult result) {
        rateLimiter.registerSuccessfulSending();
        inflight.release();
        successHandlers.forEach(h -> h.handleSuccess(message, subscription, result));
    }

    private void decrementInflightMessageCounterAfterSenderShutdown() {
        hermesMetrics.decrementInflightCounter(subscription);
    }

    private boolean messageSentSucceeded(MessageSendingResult result) {
        return result.succeeded() || (result.isClientError() && !shouldRetryOnClientError());
    }

    private boolean shouldResendMessage(MessageSendingResult result) {
        return !result.succeeded() && (!result.isClientError() || shouldRetryOnClientError()
                || isUnauthorizedForOAuthSecuredSubscription(result));
    }

    private boolean shouldRetryOnClientError() {
        return subscription.getSerialSubscriptionPolicy().isRetryClientErrors();
    }

    private boolean isUnauthorizedForOAuthSecuredSubscription(MessageSendingResult result) {
        return subscription.hasOAuthPolicy() && result.getStatusCode() == HttpStatus.UNAUTHORIZED_401;
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
            if (running) {
                if (result.succeeded()) {
                    handleMessageSendingSuccess(message, result);
                } else {
                    handleFailedSending(message, result);
                }
            } else {
                decrementInflightMessageCounterAfterSenderShutdown();
                logger.warn("Process of subscription {} is not running. " +
                                "Ignoring sending message result [successful={}, partition={}, offset={}, id={}]",
                        subscription.getQualifiedName(), result.succeeded(), message.getPartition(),
                        message.getOffset(), message.getId());
            }
        }
    }
}
