package pl.allegro.tech.hermes.consumers.consumer;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.ConsumerLatencyTimer;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.utils.FutureAsyncTimeout;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;

public class ConsumerMessageSender {

    private final ExecutorService retrySingleThreadExecutor;
    private final ExecutorService deliveryReportingExecutor;
    private final SuccessHandler successHandler;
    private final ErrorHandler errorHandler;
    private final ConsumerRateLimiter rateLimiter;
    private final MessageSender messageSender;
    private final Semaphore inflightSemaphore;
    private final HermesMetrics hermesMetrics;
    private final FutureAsyncTimeout<MessageSendingResult> async;
    private final int asyncTimeoutMs;

    private Subscription subscription;

    private volatile boolean consumerIsConsuming = true;

    public ConsumerMessageSender(Subscription subscription, MessageSender messageSender, SuccessHandler successHandler,
                                 ErrorHandler errorHandler, ConsumerRateLimiter rateLimiter, ExecutorService deliveryReportingExecutor,
                                 Semaphore inflightSemaphore, HermesMetrics hermesMetrics, int asyncTimeoutMs) {
        this.deliveryReportingExecutor = deliveryReportingExecutor;
        this.successHandler = successHandler;
        this.errorHandler = errorHandler;
        this.rateLimiter = rateLimiter;
        this.messageSender = messageSender;
        this.subscription = subscription;
        this.inflightSemaphore = inflightSemaphore;
        this.retrySingleThreadExecutor = Executors.newSingleThreadExecutor();
        this.async = new FutureAsyncTimeout<>(MessageSendingResult::failedResult);
        this.hermesMetrics = hermesMetrics;
        this.asyncTimeoutMs = asyncTimeoutMs;
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
            final ConsumerLatencyTimer consumerLatencyTimer = hermesMetrics.latencyTimer(subscription);
            try {
                submitAsyncSendMessageRequest(message, consumerLatencyTimer);
                return;
            } catch (RuntimeException e) {
                consumerLatencyTimer.stop();
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
        final CompletableFuture<MessageSendingResult> response = async.within(messageSender.send(message), Duration.ofMillis(asyncTimeoutMs));
        response.thenAcceptAsync(new DeliveryCountersReportingListener(message), deliveryReportingExecutor);
        response.thenAcceptAsync(new ResponseHandlingListener(message, consumerLatencyTimer), retrySingleThreadExecutor);
    }

    private boolean isTtlExceeded(Message message) {
        return message.isTtlExceeded(subscription.getSubscriptionPolicy().getMessageTtl());
    }

    private void handleFailedSending(Message message, MessageSendingResult result) {
        if (shouldReduceSendingRate(result)) {
            rateLimiter.registerFailedSending();
        }
        errorHandler.handleFailed(message, subscription, result);
    }

    private void handleMessageDiscarding(Message message, MessageSendingResult result) {
        inflightSemaphore.release();
        errorHandler.handleDiscarded(message, subscription, result);
    }

    private void handleMessageSendingSuccess(Message message) {
        inflightSemaphore.release();
        successHandler.handle(message, subscription);
    }

    private boolean shouldReduceSendingRate(MessageSendingResult result) {
        return shouldRetrySending(result);
    }

    private boolean shouldRetrySending(MessageSendingResult result) {
        return !result.succeeded() && (!result.isClientError() || subscription.getSubscriptionPolicy().isRetryClientErrors());
    }

    class DeliveryCountersReportingListener implements java.util.function.Consumer<MessageSendingResult> {

        private final Message message;

        public DeliveryCountersReportingListener(Message message) {
            this.message = message;
        }

        @Override
        public void accept(MessageSendingResult result) {
            if (result.succeeded()) {
                rateLimiter.registerSuccessfulSending();
            } else {
                handleFailedSending(message, result);
            }
        }
    }

    class ResponseHandlingListener implements java.util.function.Consumer<MessageSendingResult> {

        private final Message message;
        private final ConsumerLatencyTimer consumerlatencyTimer;

        public ResponseHandlingListener(Message message, ConsumerLatencyTimer consumerlatencyTimer) {
            this.message = message;
            this.consumerlatencyTimer = consumerlatencyTimer;
        }

        @Override
        public void accept(MessageSendingResult result) {
            consumerlatencyTimer.stop();
            if (result.succeeded()) {
                handleMessageSendingSuccess(message);
            } else if (!isTtlExceeded(message) && shouldRetrySending(result)) {
                sendMessage(message);
            } else {
                handleMessageDiscarding(message, result);
            }
        }
    }
}
