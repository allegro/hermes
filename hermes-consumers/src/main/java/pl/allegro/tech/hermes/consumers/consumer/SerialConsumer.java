package pl.allegro.tech.hermes.consumers.consumer;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.rate.AdjustableSemaphore;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_INFLIGHT_SIZE;
import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

public class SerialConsumer implements Consumer {

    private static final Logger logger = LoggerFactory.getLogger(SerialConsumer.class);

    private final ReceiverFactory messageReceiverFactory;
    private final HermesMetrics hermesMetrics;
    private final ConsumerRateLimiter rateLimiter;
    private final SubscriptionOffsetCommitQueues subscriptionOffsetCommitQueues;
    private final AdjustableSemaphore inflightSemaphore;
    private final Trackers trackers;
    private final MessageConverterResolver messageConverterResolver;
    private final Topic topic;
    private final ConsumerMessageSender sender;
    private final int defaultInflight;

    private Subscription subscription;

    private final CountDownLatch stoppedLatch = new CountDownLatch(1);
    private volatile boolean consuming = true;
    private MessageReceiver messageReceiver;

    public SerialConsumer(ReceiverFactory messageReceiverFactory, HermesMetrics hermesMetrics, Subscription subscription,
                          ConsumerRateLimiter rateLimiter, SubscriptionOffsetCommitQueues subscriptionOffsetCommitQueues,
                          ConsumerMessageSenderFactory consumerMessageSenderFactory, Trackers trackers,
                          MessageConverterResolver messageConverterResolver, Topic topic, ConfigFactory configFactory) {
        this.defaultInflight = configFactory.getIntProperty(CONSUMER_INFLIGHT_SIZE);
        this.inflightSemaphore = new AdjustableSemaphore(getMaxPermits(subscription));
        this.messageReceiverFactory = messageReceiverFactory;
        this.hermesMetrics = hermesMetrics;
        this.subscription = subscription;
        this.rateLimiter = rateLimiter;
        this.subscriptionOffsetCommitQueues = subscriptionOffsetCommitQueues;
        this.sender = consumerMessageSenderFactory.create(subscription, rateLimiter, subscriptionOffsetCommitQueues,
                () -> inflightSemaphore.release());
        this.trackers = trackers;
        this.messageConverterResolver = messageConverterResolver;
        this.topic = topic;
        this.messageReceiver = () -> {
            throw new IllegalStateException("Consumer not initialized");
        };
    }

    private int getMaxPermits(Subscription subscription) {
        return ofNullable(subscription.getSerialSubscriptionPolicy().getInflightSize())
                .map(value -> Math.min(value, defaultInflight))
                .orElse(defaultInflight);
    }

    private String getId() {
        return subscription.getId();
    }

    @Override
    public void run() {
        setThreadName();

        logger.info("Starting consumer for subscription {} ", subscription.getId());

        Timer.Context timer = new Timer().time();
        this.messageReceiver = initializeMessageReceiver();
        rateLimiter.initialize();

        logger.info("Started consumer for subscription {} in {} ms", subscription.getId(), TimeUnit.NANOSECONDS.toMillis(timer.stop()));

        startConsumption(messageReceiver);

        messageReceiver.stop();
        unsetThreadName();
        logger.info("Stopped consumer for subscription {}", subscription.getId());
        stoppedLatch.countDown();
    }

    private void startConsumption(MessageReceiver messageReceiver) {
        while (isConsuming()) {
            try {
                inflightSemaphore.acquire();

                Message message = messageReceiver.next();

                Message convertedMessage = messageConverterResolver.converterFor(message, subscription).convert(message, topic);

                sendMessage(convertedMessage);
            } catch (MessageReceivingTimeoutException messageReceivingTimeoutException) {
                inflightSemaphore.release();
                logger.debug("Timeout while reading message from topic. Trying to read message again", messageReceivingTimeoutException);
            } catch (Exception e) {
                logger.error("Consumer loop failed for " + getId(), e);
            }
        }
    }

    private MessageReceiver initializeMessageReceiver() {
        try {
            logger.debug("Consumer: preparing message receiver for subscription {}", subscription.getId());
            return messageReceiverFactory.createMessageReceiver(topic, subscription);
        } catch (Exception e) {
            logger.info("Failed to create consumer for subscription {} ", subscription.getId(), e);
            throw e;
        }
    }

    private void sendMessage(Message message) {
        subscriptionOffsetCommitQueues.put(message);

        hermesMetrics.incrementInflightCounter(subscription);
        trackers.get(subscription).logInflight(toMessageMetadata(message, subscription));

        sender.sendMessage(message);
    }

    public void stopConsuming() {
        logger.info("Stopping consumer for subscription {}", subscription.getId());
        rateLimiter.shutdown();
        sender.shutdown();
        consuming = false;
    }

    public void waitUntilStopped() throws InterruptedException {
        stoppedLatch.await();
    }

    public List<PartitionOffset> getOffsetsToCommit() {
        return subscriptionOffsetCommitQueues.getOffsetsToCommit();
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void updateSubscription(Subscription newSubscription) {
        inflightSemaphore.setMaxPermits(getMaxPermits(newSubscription));
        rateLimiter.updateSubscription(newSubscription);
        sender.updateSubscription(newSubscription);
        messageReceiver.update(newSubscription);
        this.subscription = newSubscription;
    }

    public boolean isConsuming() {
        return consuming;
    }

}
