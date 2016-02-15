package pl.allegro.tech.hermes.consumers.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.timer.ConsumerLatencyTimer;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
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
    private final MessageSender messageSender;
    private final Semaphore inflightSemaphore;
    private final FutureAsyncTimeout<MessageSendingResult> async;
    private final int asyncTimeoutMs;
    private ConsumerLatencyTimer consumerLatencyTimer;
    private Subscription subscription;

    private volatile boolean consumerIsConsuming = true;

    public ConsumerMessageSender(Subscription subscription, MessageSender messageSender, SuccessHandler successHandler,
                                 ErrorHandler errorHandler, ConsumerRateLimiter rateLimiter, ExecutorService deliveryReportingExecutor,
                                 Semaphore inflightSemaphore, HermesMetrics hermesMetrics, int asyncTimeoutMs,
                                 FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeout) {
        this.deliveryReportingExecutor = deliveryReportingExecutor;
        this.successHandler = successHandler;
        this.errorHandler = errorHandler;
        this.rateLimiter = rateLimiter;
        this.messageSender = messageSender;
        this.subscription = subscription;
        this.inflightSemaphore = inflightSemaphore;
        this.retrySingleThreadExecutor = Executors.newScheduledThreadPool(1);
        this.async = futureAsyncTimeout;
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
                if (isTtlExceeded(message)) {
                    handleMessageDiscarding(message, failedResult(e));
                    return;
                }
            }
        }
    }

    public void updateSubscription(Subscription newSubscription) {
        this.subscription = newSubscription;
    }

    private void submitAsyncSendMessageRequest(final Message message, final ConsumerLatencyTimer consumerLatencyTimer) {
        rateLimiter.acquire();
        ConsumerLatencyTimer.Context timer = consumerLatencyTimer.time();
        final CompletableFuture<MessageSendingResult> response = async.within(messageSender.send(message), Duration.ofMillis(asyncTimeoutMs));
        response.thenAcceptAsync(new ResponseHandlingListener(message, timer), deliveryReportingExecutor);
    }

    private boolean isTtlExceeded(Message message) {
        return message.isTtlExceeded(subscription.getSerialSubscriptionPolicy().getMessageTtl());
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
        inflightSemaphore.release();
        errorHandler.handleDiscarded(message, subscription, result);
    }

    private void handleMessageSendingSuccess(Message message, MessageSendingResult result) {
        inflightSemaphore.release();
        successHandler.handle(message, subscription, result);
    }

    private boolean shouldReduceSendingRate(MessageSendingResult result) {
        return shouldRetrySending(result);
    }

    private boolean shouldRetrySending(MessageSendingResult result) {
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
                if (!isTtlExceeded(message) && shouldRetrySending(result)) {
                    retrySingleThreadExecutor.schedule(() -> retrySending(result),
                            subscription.getSerialSubscriptionPolicy().getMessageBackoff(), TimeUnit.MILLISECONDS);
                } else {
                    handleMessageDiscarding(message, result);
                }
            }
        }

        private void retrySending(MessageSendingResult result) {
            if (result.isLoggable()) {
                logger.info(
                    format("Retrying message send to endpoint %s; messageId %s; offset: %s; partition: %s; sub id: %s; rootCause: %s",
                        subscription.getEndpoint().getEndpoint(), message.getId(), message.getOffset(), message.getPartition(),
                        subscription.getId(), result.getRootCause()),
                    result.getFailure());
            }
            sendMessage(message);
        }
    }
}
