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
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SIGNAL_PROCESSING_INTERVAL;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_INFLIGHT_SIZE;
import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

public class SerialConsumer implements Consumer {

    private static final Logger logger = LoggerFactory.getLogger(SerialConsumer.class);

    private final ReceiverFactory messageReceiverFactory;
    private final HermesMetrics hermesMetrics;
    private final ConsumerRateLimiter rateLimiter;
    private final Trackers trackers;
    private final MessageConverterResolver messageConverterResolver;
    private final ConsumerMessageSender sender;
    private final OffsetQueue offsetQueue;
    private final AdjustableSemaphore inflightSemaphore;

    private final int defaultInflight;
    private final int signalProcessingInterval;

    private Topic topic;
    private Subscription subscription;

    private MessageReceiver messageReceiver;

    public SerialConsumer(ReceiverFactory messageReceiverFactory,
                          HermesMetrics hermesMetrics,
                          Subscription subscription,
                          ConsumerRateLimiter rateLimiter,
                          ConsumerMessageSenderFactory consumerMessageSenderFactory,
                          Trackers trackers,
                          MessageConverterResolver messageConverterResolver,
                          Topic topic,
                          ConfigFactory configFactory,
                          OffsetQueue offsetQueue) {
        this.defaultInflight = configFactory.getIntProperty(CONSUMER_INFLIGHT_SIZE);
        this.signalProcessingInterval = configFactory.getIntProperty(CONSUMER_SIGNAL_PROCESSING_INTERVAL);
        this.inflightSemaphore = new AdjustableSemaphore(calculateInflightSize(subscription));
        this.messageReceiverFactory = messageReceiverFactory;
        this.hermesMetrics = hermesMetrics;
        this.subscription = subscription;
        this.rateLimiter = rateLimiter;
        this.offsetQueue = offsetQueue;
        this.sender = consumerMessageSenderFactory.create(subscription, rateLimiter, offsetQueue, inflightSemaphore::release);
        this.trackers = trackers;
        this.messageConverterResolver = messageConverterResolver;
        this.messageReceiver = () -> {
            throw new IllegalStateException("Consumer not initialized");
        };
        this.topic = topic;
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

            Message message = messageReceiver.next();
            logger.debug("Read message {} partition {} offset {}", message.getContentType(), message.getPartition(), message.getOffset());
            Message convertedMessage = messageConverterResolver.converterFor(message, subscription).convert(message, topic);
            sendMessage(convertedMessage);
        } catch (MessageReceivingTimeoutException messageReceivingTimeoutException) {
            inflightSemaphore.release();
            logger.debug("Timeout while reading message from topic. Trying to read message again", messageReceivingTimeoutException);
        } catch (Exception e) {
            logger.error("Consumer loop failed for {}", subscription.getName(), e);
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
        logger.info("Consumer: preparing message receiver for subscription {}", subscription.getName());
        initializeMessageReceiver();
        rateLimiter.initialize();
    }

    private void initializeMessageReceiver() {
        this.messageReceiver = messageReceiverFactory.createMessageReceiver(topic, subscription);
    }

    @Override
    public void tearDown() {
        messageReceiver.stop();
        rateLimiter.shutdown();
        sender.shutdown();
    }

    @Override
    public void updateSubscription(Subscription newSubscription) {
        logger.info("Updating consumer for subscription {}", subscription.getQualifiedName());
        inflightSemaphore.setMaxPermits(calculateInflightSize(newSubscription));
        rateLimiter.updateSubscription(newSubscription);
        sender.updateSubscription(newSubscription);
        messageReceiver.update(newSubscription);
        this.subscription = newSubscription;
    }

    @Override
    public void updateTopic(Topic newTopic) {
        if(this.topic.getContentType() != newTopic.getContentType()) {
            logger.info("Topic content type changed from {} to {}, reinitializing message recevier", this.topic.getContentType(), newTopic.getContentType());
            this.topic = newTopic;

            messageReceiver.stop();
            initializeMessageReceiver();
        }
    }
}
