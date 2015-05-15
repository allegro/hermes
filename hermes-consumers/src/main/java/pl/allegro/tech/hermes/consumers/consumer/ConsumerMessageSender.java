package pl.allegro.tech.hermes.consumers.consumer;

import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.LatencyTimer;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;

public class ConsumerMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerMessageSender.class);

    private final ExecutorService retrySingleThreadExecutor;
    private final ExecutorService deliveryReportingExecutor;
    private final SuccessHandler successHandler;
    private final ErrorHandler errorHandler;
    private final ConsumerRateLimiter rateLimiter;
    private final MessageSender messageSender;
    private final Semaphore inflightSemaphore;
    private final HermesMetrics hermesMetrics;

    private Subscription subscription;

    private volatile boolean consumerIsConsuming = true;

    public ConsumerMessageSender(Subscription subscription, MessageSender messageSender, SuccessHandler successHandler,
                                 ErrorHandler errorHandler, ConsumerRateLimiter rateLimiter, ExecutorService deliveryReportingExecutor,
                                 Semaphore inflightSemaphore, HermesMetrics hermesMetrics) {
        this.deliveryReportingExecutor = deliveryReportingExecutor;
        this.successHandler = successHandler;
        this.errorHandler = errorHandler;
        this.rateLimiter = rateLimiter;
        this.messageSender = messageSender;
        this.subscription = subscription;
        this.inflightSemaphore = inflightSemaphore;
        this.retrySingleThreadExecutor = Executors.newSingleThreadExecutor();
        this.hermesMetrics = hermesMetrics;
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
            final LatencyTimer latencyTimer = hermesMetrics.latencyTimer(subscription);
            try {
                submitAsyncSendMessageRequest(message, latencyTimer);
                return;
            } catch (RuntimeException e) {
                latencyTimer.stop();
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

    private void submitAsyncSendMessageRequest(final Message message, final LatencyTimer latencyTimer) {
        rateLimiter.acquire();
        final ListenableFuture<MessageSendingResult> response = messageSender.send(message);
        response.addListener(new DeliveryCountersReportingListener(message, response), deliveryReportingExecutor);
        response.addListener(new ResponseHandlingListener(message, response, latencyTimer), retrySingleThreadExecutor);
    }

    private boolean isTtlExceeded(Message message) {
        return message.isTttlExceeded(subscription.getSubscriptionPolicy().getMessageTtl());
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

    private MessageSendingResult fetchResult(ListenableFuture<MessageSendingResult> result) {
        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.debug("Problem while sending message", e);
            return failedResult(e);
        }
    }

    class DeliveryCountersReportingListener implements Runnable {

        private final Message message;
        private final ListenableFuture<MessageSendingResult> result;

        public DeliveryCountersReportingListener(Message message, ListenableFuture<MessageSendingResult> res) {
            this.message = message;
            this.result = res;
        }

        @Override
        public void run() {
            MessageSendingResult result = fetchResult(this.result);
            if (result.succeeded()) {
                rateLimiter.registerSuccessfulSending();
            } else {
                handleFailedSending(message, result);
            }
        }
    }

    class ResponseHandlingListener implements Runnable {

        private final ListenableFuture<MessageSendingResult> response;
        private final Message message;
        private final LatencyTimer latencyTimer;

        public ResponseHandlingListener(Message message, ListenableFuture<MessageSendingResult> response, LatencyTimer latencyTimer) {
            this.message = message;
            this.response = response;
            this.latencyTimer = latencyTimer;
        }

        @Override
        public void run() {
            MessageSendingResult result = fetchResult(response);
            latencyTimer.stop();
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
