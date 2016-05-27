package pl.allegro.tech.hermes.consumers.consumer;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;
import static pl.allegro.tech.hermes.consumers.consumer.Message.message;
import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

public class SerialConsumer implements Consumer {

    private static final Logger logger = LoggerFactory.getLogger(SerialConsumer.class);

    private final ReceiverFactory messageReceiverFactory;
    private final HermesMetrics hermesMetrics;
    private final ConsumerRateLimiter rateLimiter;
    private final SubscriptionOffsetCommitQueues subscriptionOffsetCommitQueues;
    private final Semaphore inflightSemaphore;
    private final Trackers trackers;
    private final MessageConverterResolver messageConverterResolver;
    private final Topic topic;
    private final ConsumerMessageSender sender;

    private Subscription subscription;

    private final CountDownLatch stoppedLatch = new CountDownLatch(1);
    private volatile boolean consuming = true;
    private MessageReceiver messageReceiver;

    public SerialConsumer(ReceiverFactory messageReceiverFactory, HermesMetrics hermesMetrics, Subscription subscription,
                          ConsumerRateLimiter rateLimiter, SubscriptionOffsetCommitQueues subscriptionOffsetCommitQueues,
                          ConsumerMessageSender sender, Semaphore inflightSemaphore, Trackers trackers,
                          MessageConverterResolver messageConverterResolver, Topic topic) {
        this.messageReceiverFactory = messageReceiverFactory;
        this.hermesMetrics = hermesMetrics;
        this.subscription = subscription;
        this.rateLimiter = rateLimiter;
        this.subscriptionOffsetCommitQueues = subscriptionOffsetCommitQueues;
        this.sender = sender;
        this.inflightSemaphore = inflightSemaphore;
        this.trackers = trackers;
        this.messageConverterResolver = messageConverterResolver;
        this.topic = topic;
        this.messageReceiver = () -> {
            throw new IllegalStateException("Consumer not initialized");
        };
    }

    private String getId() {
        return subscription.getId();
    }

    @Override
    public void run() {
        try {
            setThreadName();

            logger.info("Starting consumer for subscription {} ", subscription.getId());

            Timer.Context timer = new Timer().time();
            this.messageReceiver = initializeMessageReceiver();
            rateLimiter.initialize();

            logger.info("Started consumer for subscription {} in {} ms", subscription.getId(), TimeUnit.NANOSECONDS.toMillis(timer.stop()));

            startConsumption(messageReceiver);
        } finally {
            unsetThreadName();
            ofNullable(messageReceiver).ifPresent(MessageReceiver::stop);
            logger.info("Stopped consumer for subscription {}", subscription.getId());
            stoppedLatch.countDown();
        }
    }

    private void startConsumption(MessageReceiver messageReceiver) {
        while (isConsuming()) {
            try {
                inflightSemaphore.acquire();

                Message message = messageReceiver.next();

                Message convertedMessage = messageConverterResolver.converterFor(message, subscription).convert(message, topic);

                sendMessage(withSubscriptionHeaders(convertedMessage));
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

    private Message withSubscriptionHeaders(Message message) {
        if (subscription.getHeaders().isEmpty()) {
            return message;
        }
        return message().fromMessage(message)
                .withAdditionalHeaders(subscription.getHeaders())
                .build();
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
        rateLimiter.updateSubscription(newSubscription);
        sender.updateSubscription(newSubscription);
        messageReceiver.update(newSubscription);
        this.subscription = newSubscription;
    }

    public boolean isConsuming() {
        return consuming;
    }

}
