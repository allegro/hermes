package pl.allegro.tech.hermes.consumers.consumer;

import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.common.metric.Timers;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.consumers.consumer.receiver.SplitMessagesReceiver;
import pl.allegro.tech.hermes.consumers.message.tracker.Trackers;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import java.util.List;
import java.util.concurrent.Semaphore;

public class Consumer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);

    private final SplitMessagesReceiver messageReceiver;
    private final HermesMetrics hermesMetrics;
    private final ConsumerRateLimiter rateLimiter;
    private final SubscriptionOffsetCommitQueues subscriptionOffsetCommitQueues;
    private final Semaphore inflightSemaphore;
    private final Trackers trackers;
    private final ConsumerMessageSender sender;

    private Subscription subscription;

    private volatile boolean consuming = true;

    public Consumer(SplitMessagesReceiver messageReceiver, HermesMetrics hermesMetrics, Subscription subscription,
                    ConsumerRateLimiter rateLimiter, SubscriptionOffsetCommitQueues subscriptionOffsetCommitQueues, ConsumerMessageSender sender,
                    Semaphore inflightSemaphore, Trackers trackers) {
        this.messageReceiver = messageReceiver;
        this.hermesMetrics = hermesMetrics;
        this.subscription = subscription;
        this.rateLimiter = rateLimiter;
        this.subscriptionOffsetCommitQueues = subscriptionOffsetCommitQueues;
        this.sender = sender;
        this.inflightSemaphore = inflightSemaphore;
        this.trackers = trackers;
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

                List<Message> messages = readMessages();

                if (messages.isEmpty()) {
                    inflightSemaphore.release();
                }

                sendMessages(messages);
            } catch (Exception e) {
                LOGGER.error("Consumer loop failed for " + getId(), e);
            }
        }
        LOGGER.info("Stopping consumer for subscription {}", subscription.getId());
        messageReceiver.stop();
    }

    private List<Message> readMessages() {
        Timer.Context ctx = hermesMetrics.timer(Timers.CONSUMER_READ_LATENCY).time();
        List<Message> messages = messageReceiver.next();
        ctx.close();
        return messages;
    }

    private void sendMessages(List<Message> messages) {
        for (Message message : messages) {
            subscriptionOffsetCommitQueues.put(message);

            hermesMetrics.incrementInflightCounter(subscription);
            trackers.get(subscription).logInflight(message, subscription);

            sender.sendMessage(message);
        }
    }

    public void stopConsuming() {
        rateLimiter.shutdown();
        sender.shutdown();
        consuming = false;
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

    private void setThreadName() {
        Thread.currentThread().setName("Consumer-" + subscription.getId());
    }

    @VisibleForTesting
    protected boolean isConsuming() {
        return consuming;
    }

}
