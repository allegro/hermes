package pl.allegro.tech.hermes.consumers.consumer;

import com.codahale.metrics.Timer;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.BatchSubscriptionPolicy;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.batch.BatchMonitoring;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchFactory;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchReceiver;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchingResult;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.rholder.retry.WaitStrategies.fixedWait;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class BatchConsumer implements Consumer {
    private static final Logger logger = LoggerFactory.getLogger(BatchConsumer.class);

    private final ReceiverFactory messageReceiverFactory;
    private final MessageBatchSender sender;
    private final MessageBatchFactory batchFactory;
    private final SubscriptionOffsetCommitQueues offsets;
    private final CountDownLatch stoppedLatch = new CountDownLatch(1);
    private final HermesMetrics hermesMetrics;
    private final MessageConverterResolver messageConverterResolver;
    private final MessageContentWrapper messageContentWrapper;
    private final Topic topic;
    private final Trackers trackers;
    private Optional<MessageBatchReceiver> receiver;

    private Subscription subscription;
    private volatile boolean consuming = true;

    private BatchMonitoring monitoring;

    public BatchConsumer(ReceiverFactory messageReceiverFactory,
                         MessageBatchSender sender,
                         MessageBatchFactory batchFactory,
                         SubscriptionOffsetCommitQueues offsets,
                         MessageConverterResolver messageConverterResolver,
                         MessageContentWrapper messageContentWrapper,
                         HermesMetrics hermesMetrics,
                         Trackers trackers,
                         Subscription subscription,
                         Topic topic) {
        this.messageReceiverFactory = messageReceiverFactory;
        this.sender = sender;
        this.batchFactory = batchFactory;
        this.offsets = offsets;
        this.subscription = subscription;
        this.hermesMetrics = hermesMetrics;
        this.monitoring = new BatchMonitoring(hermesMetrics, trackers);
        this.messageConverterResolver = messageConverterResolver;
        this.messageContentWrapper = messageContentWrapper;
        this.topic = topic;
        this.trackers = trackers;
    }

    @Override
    public void run() {
        setThreadName();
        logger.info("Starting batch consumer for subscription {} ", subscription.getId());
        Timer.Context timer = new Timer().time();
        receiver = Optional.of(initializeMessageReceiver());
        logger.info("Started batch consumer for subscription {} in {} ms", subscription.getId(), TimeUnit.NANOSECONDS.toMillis(timer.stop()));
        try {
            consume();
        } finally {
            logger.info("Stopped consumer for subscription {}", subscription.getId());
            unsetThreadName();
            stoppedLatch.countDown();
        }
    }

    private MessageBatchReceiver initializeMessageReceiver() {
        try {
            logger.debug("Consumer: preparing receiver for subscription {}", subscription.getId());
            MessageReceiver receiver = messageReceiverFactory.createMessageReceiver(topic, subscription);
            logger.debug("Consumer: preparing batch receiver for subscription {}", subscription.getId());
            return new MessageBatchReceiver(receiver, batchFactory, hermesMetrics, messageConverterResolver, messageContentWrapper, topic, trackers);
        } catch (Exception e) {
            logger.info("Failed to create consumer for subscription {} ", subscription.getId(), e);
            throw e;
        }
    }

    private void consume() {
        while (isConsuming()) {
            Optional<MessageBatch> inflight = Optional.empty();
            try {
                logger.debug("Trying to create new batch [subscription={}].", subscription.getId());

                MessageBatchingResult result = receiver.get().next(subscription);
                inflight = of(result.getBatch());
                inflight.ifPresent(batch -> {
                    logger.debug("Delivering batch [subscription={}].", subscription.getId());
                    deliver(batch, createRetryer(batch, subscription.getBatchSubscriptionPolicy()));
                    logger.debug("Finished delivering batch [subscription={}]", subscription.getId());
                    offsets.putAllDelivered(batch.getPartitionOffsets());
                });
                result.getDiscarded().forEach(m -> monitoring.markDiscarded(m, subscription, "too large"));
            } finally {
                logger.debug("Cleaning batch [subscription={}]", subscription.getId());
                inflight.ifPresent(this::clean);
            }
        }
    }

    private Retryer<MessageSendingResult> createRetryer(MessageBatch batch, BatchSubscriptionPolicy policy) {
        return createRetryer(batch, policy.getMessageBackoff(), policy.getMessageTtl(), policy.isRetryClientErrors());
    }

    private Retryer<MessageSendingResult> createRetryer(final MessageBatch batch, int messageBackoff, int messageTtl, boolean retryClientErrors) {
        return RetryerBuilder.<MessageSendingResult>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .retryIfResult(result -> isConsuming() && !result.succeeded() && shouldRetryOnClientError(retryClientErrors, result))
                .withWaitStrategy(fixedWait(messageBackoff, MILLISECONDS))
                .withStopStrategy(attempt -> attempt.getDelaySinceFirstAttempt() > messageTtl)
                .withRetryListener(getRetryListener(result -> {
                    batch.incrementRetryCounter();
                    monitoring.markFailed(batch, subscription, result);
                }))
                .build();
    }

    private boolean shouldRetryOnClientError(boolean retryClientErrors, MessageSendingResult result) {
        return !result.isClientError() || retryClientErrors;
    }

    private void deliver(MessageBatch batch, Retryer<MessageSendingResult> retryer) {
        try (Timer.Context timer = hermesMetrics.subscriptionLatencyTimer(subscription).time()) {
            MessageSendingResult result = retryer.call(() -> sender.send(batch, subscription.getEndpoint(), subscription.getBatchSubscriptionPolicy().getRequestTimeout()));
            monitoring.markSendingResult(batch, subscription, result);
        } catch (Exception e) {
            logger.error("Batch was rejected [batch_id={}, subscription={}].", batch.getId(), subscription.toSubscriptionName(), e);
            monitoring.markDiscarded(batch, subscription, e.getMessage());
        }
    }

    private void clean(MessageBatch batch) {
        batchFactory.destroyBatch(batch);
        monitoring.closeInflightMetrics(batch, subscription);
    }

    @Override
    public Subscription getSubscription() {
        return subscription;
    }

    @Override
    public void updateSubscription(Subscription modifiedSubscription) {
        this.subscription = modifiedSubscription;
        this.receiver.ifPresent(r -> r.updateSubscription(modifiedSubscription));
    }

    @Override
    public void stopConsuming() {
        logger.info("Stopping consumer [subscription={}].", subscription.getId());
        this.receiver.ifPresent(MessageBatchReceiver::stop);
        consuming = false;
    }

    @Override
    public void waitUntilStopped() throws InterruptedException {
        stoppedLatch.await();
    }

    @Override
    public List<PartitionOffset> getOffsetsToCommit() {
        return offsets.getOffsetsToCommit();
    }

    @Override
    public boolean isConsuming() {
        return consuming;
    }

    private RetryListener getRetryListener(java.util.function.Consumer<MessageSendingResult> consumer) {
        return new RetryListener() {
            @Override
            public <V> void onRetry(Attempt<V> attempt) {
                consumer.accept((MessageSendingResult) attempt.getResult());
            }
        };
    }
}
