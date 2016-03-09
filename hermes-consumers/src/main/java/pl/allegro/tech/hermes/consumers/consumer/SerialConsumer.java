package pl.allegro.tech.hermes.consumers.consumer;

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
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

public class SerialConsumer implements Consumer {

    private static final Logger logger = LoggerFactory.getLogger(SerialConsumer.class);

    private final MessageReceiver messageReceiver;
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

    public SerialConsumer(MessageReceiver messageReceiver, HermesMetrics hermesMetrics, Subscription subscription,
                          ConsumerRateLimiter rateLimiter, SubscriptionOffsetCommitQueues subscriptionOffsetCommitQueues,
                          ConsumerMessageSender sender, Semaphore inflightSemaphore, Trackers trackers,
                          MessageConverterResolver messageConverterResolver, Topic topic) {
        this.messageReceiver = messageReceiver;
        this.hermesMetrics = hermesMetrics;
        this.subscription = subscription;
        this.rateLimiter = rateLimiter;
        this.subscriptionOffsetCommitQueues = subscriptionOffsetCommitQueues;
        this.sender = sender;
        this.inflightSemaphore = inflightSemaphore;
        this.trackers = trackers;
        this.messageConverterResolver = messageConverterResolver;
        this.topic = topic;
    }

    private String getId() {
        return subscription.getId();
    }

    @Override
    public void run() {
        setThreadName();
        rateLimiter.initialize();
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
        messageReceiver.stop();
        unsetThreadName();
        logger.info("Stopped consumer for subscription {}", subscription.getId());
        stoppedLatch.countDown();
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
        this.subscription = newSubscription;
    }

    public boolean isConsuming() {
        return consuming;
    }

}
