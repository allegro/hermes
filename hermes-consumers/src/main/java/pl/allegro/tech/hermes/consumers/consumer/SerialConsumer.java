package pl.allegro.tech.hermes.consumers.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.CommonConsumerParameters;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.rate.AdjustableSemaphore;
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.UninitializedMessageReceiver;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;

public class SerialConsumer implements Consumer {

    private static final Logger logger = LoggerFactory.getLogger(SerialConsumer.class);

    private final ReceiverFactory messageReceiverFactory;
    private final MetricsFacade metrics;
    private final SerialConsumerRateLimiter rateLimiter;
    private final Trackers trackers;
    private final MessageConverterResolver messageConverterResolver;
    private final ConsumerMessageSender sender;
    private final boolean useTopicMessageSizeEnabled;
    private final OffsetQueue offsetQueue;
    private final ConsumerAuthorizationHandler consumerAuthorizationHandler;
    private final AdjustableSemaphore inflightSemaphore;
    private final SubscriptionLoadRecorder loadRecorder;

    private final int defaultInflight;
    private final Duration signalProcessingInterval;

    private Topic topic;
    private Subscription subscription;

    private MessageReceiver messageReceiver;

    public SerialConsumer(ReceiverFactory messageReceiverFactory,
                          MetricsFacade metrics,
                          Subscription subscription,
                          SerialConsumerRateLimiter rateLimiter,
                          ConsumerMessageSenderFactory consumerMessageSenderFactory,
                          Trackers trackers,
                          MessageConverterResolver messageConverterResolver,
                          Topic topic,
                          CommonConsumerParameters commonConsumerParameters,
                          OffsetQueue offsetQueue,
                          ConsumerAuthorizationHandler consumerAuthorizationHandler,
                          SubscriptionLoadRecorder loadRecorder) {

        this.defaultInflight = commonConsumerParameters.getSerialConsumer().getInflightSize();
        this.signalProcessingInterval = commonConsumerParameters.getSerialConsumer().getSignalProcessingInterval();
        this.inflightSemaphore = new AdjustableSemaphore(calculateInflightSize(subscription));
        this.messageReceiverFactory = messageReceiverFactory;
        this.metrics = metrics;
        this.subscription = subscription;
        this.rateLimiter = rateLimiter;
        this.useTopicMessageSizeEnabled = commonConsumerParameters.isUseTopicMessageSizeEnabled();
        this.offsetQueue = offsetQueue;
        this.consumerAuthorizationHandler = consumerAuthorizationHandler;
        this.trackers = trackers;
        this.messageConverterResolver = messageConverterResolver;
        this.loadRecorder = loadRecorder;
        this.messageReceiver = new UninitializedMessageReceiver();
        this.topic = topic;
        this.sender = consumerMessageSenderFactory.create(
                subscription,
                rateLimiter,
                offsetQueue,
                inflightSemaphore::release,
                loadRecorder,
                metrics
        );
    }

    private int calculateInflightSize(Subscription subscription) {
        Optional<Integer> subscriptionInflight = Optional.ofNullable(subscription.getSerialSubscriptionPolicy().getInflightSize());
        return subscriptionInflight.orElse(defaultInflight);
    }

    @Override
    public void consume(Runnable signalsInterrupt) {
        try {
            do {
                loadRecorder.recordSingleOperation();
                signalsInterrupt.run();
            } while (!inflightSemaphore.tryAcquire(signalProcessingInterval.toMillis(), TimeUnit.MILLISECONDS));

            Optional<Message> maybeMessage = messageReceiver.next();

            if (maybeMessage.isPresent()) {
                Message message = maybeMessage.get();

                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Read message {} partition {} offset {}",
                            message.getContentType(), message.getPartition(), message.getOffset()
                    );
                }

                Message convertedMessage = messageConverterResolver.converterFor(message, subscription).convert(message, topic);
                sendMessage(convertedMessage);
            } else {
                inflightSemaphore.release();
            }
        } catch (InterruptedException e) {
            logger.info("Restoring interrupted status {}", subscription.getQualifiedName(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Consumer loop failed for {}", subscription.getQualifiedName(), e);
        }
    }

    private void sendMessage(Message message) {
        offsetQueue.offerInflightOffset(
                subscriptionPartitionOffset(subscription.getQualifiedName(),
                message.getPartitionOffset(),
                message.getPartitionAssignmentTerm())
        );

        trackers.get(subscription).logInflight(toMessageMetadata(message, subscription));

        sender.sendAsync(message);
    }

    @Override
    public void initialize() {
        logger.info("Consumer: preparing message receiver for subscription {}", subscription.getQualifiedName());
        initializeMessageReceiver();
        sender.initialize();
        rateLimiter.initialize();
        loadRecorder.initialize();
        consumerAuthorizationHandler.createSubscriptionHandler(subscription.getQualifiedName());
    }

    private void initializeMessageReceiver() {
        this.messageReceiver = messageReceiverFactory.createMessageReceiver(topic, subscription, rateLimiter, loadRecorder, metrics);
    }

    /**
     * Try to keep shutdown order the same as initialization so nothing will left to clean up when error occurs during initialization.
     */
    @Override
    public void tearDown() {
        messageReceiver.stop();
        sender.shutdown();
        rateLimiter.shutdown();
        loadRecorder.shutdown();
        consumerAuthorizationHandler.removeSubscriptionHandler(subscription.getQualifiedName());
        metrics.unregisterAllMetricsRelatedTo(subscription.getQualifiedName());
    }

    @Override
    public void updateSubscription(Subscription newSubscription) {
        logger.info("Updating consumer for subscription {}", subscription.getQualifiedName());
        inflightSemaphore.setMaxPermits(calculateInflightSize(newSubscription));
        rateLimiter.updateSubscription(newSubscription);
        sender.updateSubscription(newSubscription);
        messageReceiver.update(newSubscription);
        consumerAuthorizationHandler.updateSubscription(newSubscription.getQualifiedName());
        this.subscription = newSubscription;
    }

    @Override
    public void updateTopic(Topic newTopic) {
        if (this.topic.getContentType() != newTopic.getContentType()
                || messageSizeChanged(newTopic)
                || this.topic.isSchemaIdAwareSerializationEnabled() != newTopic.isSchemaIdAwareSerializationEnabled()) {
            logger.info("Reinitializing message receiver, contentType, messageSize or schemaIdAwareSerialization changed.");
            this.topic = newTopic;

            messageReceiver.stop();
            initializeMessageReceiver();
        }
    }

    private boolean messageSizeChanged(Topic newTopic) {
        return this.topic.getMaxMessageSize() != newTopic.getMaxMessageSize()
                && useTopicMessageSizeEnabled;
    }

    @Override
    public void commit(Set<SubscriptionPartitionOffset> offsets) {
        messageReceiver.commit(offsets);
    }

    @Override
    public boolean moveOffset(PartitionOffset offset) {
        return messageReceiver.moveOffset(offset);
    }

    @Override
    public Subscription getSubscription() {
        return subscription;
    }
}
