package pl.allegro.tech.hermes.consumers.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.rate.AdjustableSemaphore;
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.UninitializedMessageReceiver;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_INFLIGHT_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SIGNAL_PROCESSING_INTERVAL;
import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

public class SerialConsumer implements Consumer {

    private static final Logger logger = LoggerFactory.getLogger(SerialConsumer.class);

    private final ReceiverFactory messageReceiverFactory;
    private final HermesMetrics hermesMetrics;
    private final SerialConsumerRateLimiter rateLimiter;
    private final Trackers trackers;
    private final MessageConverterResolver messageConverterResolver;
    private final ConsumerMessageSender sender;
    private final OffsetQueue offsetQueue;
    private final ConsumerAuthorizationHandler consumerAuthorizationHandler;
    private final AdjustableSemaphore inflightSemaphore;

    private final int defaultInflight;
    private final int signalProcessingInterval;

    private Topic topic;
    private Subscription subscription;

    private MessageReceiver messageReceiver;

    public SerialConsumer(ReceiverFactory messageReceiverFactory,
                          HermesMetrics hermesMetrics,
                          Subscription subscription,
                          SerialConsumerRateLimiter rateLimiter,
                          ConsumerMessageSenderFactory consumerMessageSenderFactory,
                          Trackers trackers,
                          MessageConverterResolver messageConverterResolver,
                          Topic topic,
                          ConfigFactory configFactory,
                          OffsetQueue offsetQueue,
                          ConsumerAuthorizationHandler consumerAuthorizationHandler) {

        this.defaultInflight = configFactory.getIntProperty(CONSUMER_INFLIGHT_SIZE);
        this.signalProcessingInterval = configFactory.getIntProperty(CONSUMER_SIGNAL_PROCESSING_INTERVAL);
        this.inflightSemaphore = new AdjustableSemaphore(calculateInflightSize(subscription));
        this.messageReceiverFactory = messageReceiverFactory;
        this.hermesMetrics = hermesMetrics;
        this.subscription = subscription;
        this.rateLimiter = rateLimiter;
        this.offsetQueue = offsetQueue;
        this.consumerAuthorizationHandler = consumerAuthorizationHandler;
        this.trackers = trackers;
        this.messageConverterResolver = messageConverterResolver;
        this.messageReceiver = new UninitializedMessageReceiver();
        this.topic = topic;
        this.sender = consumerMessageSenderFactory.create(subscription, rateLimiter, offsetQueue, inflightSemaphore::release);
    }

    private int calculateInflightSize(Subscription subscription) {
        return Math.min(
                subscription.getSerialSubscriptionPolicy().getInflightSize(),
                defaultInflight
        );
    }

    @Override
    public void consume(Runnable signalsInterrupt) {
        try {
            do {
                signalsInterrupt.run();
            } while (!inflightSemaphore.tryAcquire(signalProcessingInterval, TimeUnit.MILLISECONDS));

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
        } catch (Exception e) {
            logger.error("Consumer loop failed for {}", subscription.getQualifiedName(), e);
        }
    }

    private void sendMessage(Message message) {
        offsetQueue.offerInflightOffset(SubscriptionPartitionOffset.subscriptionPartitionOffset(message, subscription));

        hermesMetrics.incrementInflightCounter(subscription);
        trackers.get(subscription).logInflight(toMessageMetadata(message, subscription));

        sender.sendAsync(message);
    }

    @Override
    public void initialize() {
        logger.info("Consumer: preparing message receiver for subscription {}", subscription.getQualifiedName());
        initializeMessageReceiver();
        sender.initialize();
        rateLimiter.initialize();
        consumerAuthorizationHandler.createSubscriptionHandler(subscription.getQualifiedName());
    }

    private void initializeMessageReceiver() {
        this.messageReceiver = messageReceiverFactory.createMessageReceiver(topic, subscription, rateLimiter);
    }

    @Override
    public void tearDown() {
        messageReceiver.stop();
        rateLimiter.shutdown();
        sender.shutdown();
        consumerAuthorizationHandler.removeSubscriptionHandler(subscription.getQualifiedName());
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
        if (this.topic.getContentType() != newTopic.getContentType()) {
            logger.info("Topic content type changed from {} to {}, reinitializing message receiver",
                    this.topic.getContentType(), newTopic.getContentType());
            this.topic = newTopic;

            messageReceiver.stop();
            initializeMessageReceiver();
        }
    }

    @Override
    public void commit(Set<SubscriptionPartitionOffset> offsets) {
        messageReceiver.commit(offsets);
    }

    @Override
    public void moveOffset(SubscriptionPartitionOffset offset) {
        messageReceiver.moveOffset(offset);
    }
}
