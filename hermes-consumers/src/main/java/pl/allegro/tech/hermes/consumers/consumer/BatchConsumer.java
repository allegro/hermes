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
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.batch.BatchMonitoring;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchFactory;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchReceiver;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchingResult;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.rate.BatchConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static com.github.rholder.retry.WaitStrategies.fixedWait;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class BatchConsumer implements Consumer {

    private static final Logger logger = LoggerFactory.getLogger(BatchConsumer.class);

    private final ReceiverFactory messageReceiverFactory;
    private final MessageBatchSender sender;
    private final MessageBatchFactory batchFactory;
    private final HermesMetrics hermesMetrics;
    private final ConfigFactory configs;
    private final MessageConverterResolver messageConverterResolver;
    private final MessageContentWrapper messageContentWrapper;
    private final Trackers trackers;

    private Topic topic;
    private OffsetQueue offsetQueue;
    private Subscription subscription;

    private volatile boolean consuming = true;

    private BatchMonitoring monitoring;
    private MessageBatchReceiver receiver;

    public BatchConsumer(ReceiverFactory messageReceiverFactory,
                         MessageBatchSender sender,
                         MessageBatchFactory batchFactory,
                         OffsetQueue offsetQueue,
                         MessageConverterResolver messageConverterResolver,
                         MessageContentWrapper messageContentWrapper,
                         HermesMetrics hermesMetrics,
                         Trackers trackers,
                         Subscription subscription,
                         Topic topic,
                         ConfigFactory configs) {
        this.messageReceiverFactory = messageReceiverFactory;
        this.sender = sender;
        this.batchFactory = batchFactory;
        this.offsetQueue = offsetQueue;
        this.subscription = subscription;
        this.hermesMetrics = hermesMetrics;
        this.configs = configs;
        this.monitoring = new BatchMonitoring(hermesMetrics, trackers);
        this.messageConverterResolver = messageConverterResolver;
        this.messageContentWrapper = messageContentWrapper;
        this.topic = topic;
        this.trackers = trackers;
    }

    @Override
    public void consume(Runnable signalsInterrupt) {
        Optional<MessageBatch> inflight = Optional.empty();
        try {
            logger.debug("Trying to create new batch [subscription={}].", subscription.getQualifiedName());

            signalsInterrupt.run();

            MessageBatchingResult result = receiver.next(subscription, signalsInterrupt);
            inflight = of(result.getBatch());

            inflight.ifPresent(batch -> {
                logger.debug("Delivering batch [subscription={}].", subscription.getQualifiedName());
                offerInflightOffsets(batch);

                deliver(signalsInterrupt, batch, createRetryer(batch, subscription.getBatchSubscriptionPolicy()));

                offerCommittedOffsets(batch);
                logger.debug("Finished delivering batch [subscription={}]", subscription.getQualifiedName());
            });

            result.getDiscarded().forEach(m -> monitoring.markDiscarded(m, subscription, "too large"));
        } finally {
            logger.debug("Cleaning batch [subscription={}]", subscription.getQualifiedName());
            inflight.ifPresent(this::clean);
        }
    }

    private void offerInflightOffsets(MessageBatch batch) {
        batch.getPartitionOffsets().forEach(offsetQueue::offerInflightOffset);
    }

    private void offerCommittedOffsets(MessageBatch batch) {
        batch.getPartitionOffsets().forEach(offsetQueue::offerCommittedOffset);
    }

    @Override
    public void initialize() {
        logger.debug("Consumer: preparing receiver for subscription {}", subscription.getQualifiedName());
        MessageReceiver receiver = messageReceiverFactory.createMessageReceiver(topic, subscription, new BatchConsumerRateLimiter());

        logger.debug("Consumer: preparing batch receiver for subscription {}", subscription.getQualifiedName());
        this.receiver = new MessageBatchReceiver(receiver, batchFactory, hermesMetrics, messageConverterResolver, messageContentWrapper, topic, trackers);
    }

    @Override
    public void tearDown() {
        consuming = false;
        if (receiver != null) {
            receiver.stop();
        } else {
            logger.info("No batch receiver to stop [subscription={}].", subscription.getQualifiedName());
        }
    }

    @Override
    public void updateSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void updateTopic(Topic newTopic) {
        if (this.topic.getContentType() != newTopic.getContentType() || messageSizeChanged(newTopic)) {
            logger.info("Reinitializing message receiver, contentType or messageSize changed.");
            this.topic = newTopic;
            tearDown();
            initialize();
        }
    }

    private boolean messageSizeChanged(Topic newTopic) {
        return this.topic.getMaxMessageSize() != newTopic.getMaxMessageSize()
                && configs.getBooleanProperty(Configs.CONSUMER_USE_TOPIC_MESSAGE_SIZE);
    }

    @Override
    public void commit(Set<SubscriptionPartitionOffset> offsetsToCommit) {
        if (receiver != null) {
            receiver.commit(offsetsToCommit);
        }
    }

    @Override
    public boolean moveOffset(PartitionOffset partitionOffset) {
        if (receiver != null) {
            return receiver.moveOffset(partitionOffset);
        }
        return false;
    }

    @Override
    public Subscription getSubscription() {
        return subscription;
    }

    private Retryer<MessageSendingResult> createRetryer(MessageBatch batch, BatchSubscriptionPolicy policy) {
        return createRetryer(batch,
                policy.getMessageBackoff(),
                SECONDS.toMillis(policy.getMessageTtl()),
                policy.isRetryClientErrors());
    }

    private Retryer<MessageSendingResult> createRetryer(final MessageBatch batch,
                                                        int messageBackoff,
                                                        long messageTtlMillis,
                                                        boolean retryClientErrors) {
        return RetryerBuilder.<MessageSendingResult>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .retryIfResult(result -> consuming && !result.succeeded() && shouldRetryOnClientError(retryClientErrors, result))
                .withWaitStrategy(fixedWait(messageBackoff, MILLISECONDS))
                .withStopStrategy(attempt -> attempt.getDelaySinceFirstAttempt() > messageTtlMillis)
                .withRetryListener(getRetryListener(result -> {
                    batch.incrementRetryCounter();
                    monitoring.markSendingResult(batch, subscription, result);
                }))
                .build();
    }

    private boolean shouldRetryOnClientError(boolean retryClientErrors, MessageSendingResult result) {
        return !result.isClientError() || retryClientErrors;
    }

    private void deliver(Runnable signalsInterrupt, MessageBatch batch, Retryer<MessageSendingResult> retryer) {
        try (Timer.Context timer = hermesMetrics.subscriptionLatencyTimer(subscription).time()) {
            retryer.call(() -> {
                signalsInterrupt.run();
                return sender.send(
                        batch,
                        subscription.getEndpoint(),
                        subscription.getEndpointAddressResolverMetadata(),
                        subscription.getBatchSubscriptionPolicy().getRequestTimeout()
                );
            });
        } catch (Exception e) {
            logger.error("Batch was rejected [batch_id={}, subscription={}].", batch.getId(), subscription.getQualifiedName(), e);
            monitoring.markDiscarded(batch, subscription, e.getMessage());
        }
    }

    private void clean(MessageBatch batch) {
        batchFactory.destroyBatch(batch);
        monitoring.closeInflightMetrics(batch, subscription);
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
