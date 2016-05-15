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
import pl.allegro.tech.hermes.consumers.consumer.status.MutableStatus;
import pl.allegro.tech.hermes.consumers.consumer.status.Status;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;
import static pl.allegro.tech.hermes.consumers.consumer.status.Status.ShutdownCause.*;
import static pl.allegro.tech.hermes.consumers.consumer.status.Status.StatusType.*;

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
    private final Clock clock;
    private final ConsumerMessageSender sender;

    private Subscription subscription;

    private final CountDownLatch stoppedLatch = new CountDownLatch(1);
    private volatile boolean consuming = true;
    private MessageReceiver messageReceiver;
    private volatile boolean restart = false;

    private final BlockingQueue<Runnable> commands = new ArrayBlockingQueue<>(100);

    private final MutableStatus status;

    public SerialConsumer(ReceiverFactory messageReceiverFactory, HermesMetrics hermesMetrics, Subscription subscription,
                          ConsumerRateLimiter rateLimiter, SubscriptionOffsetCommitQueues subscriptionOffsetCommitQueues,
                          ConsumerMessageSender sender, Semaphore inflightSemaphore, Trackers trackers,
                          MessageConverterResolver messageConverterResolver, Topic topic, Clock clock) {
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
        this.clock = clock;
        this.status = new MutableStatus(clock);
    }

    private String getId() {
        return subscription.getId();
    }

    @Override
    public void run() {
        try {
            setThreadName();
            restart = false;
            status.set(STARTING);
            logger.info("Starting consumer for subscription {} ", subscription.getId());
            Timer.Context timer = new Timer().time();
            this.messageReceiver = initializeMessageReceiver();
            rateLimiter.initialize();
            logger.info("Started consumer for subscription {} in {} ms", subscription.getId(), TimeUnit.NANOSECONDS.toMillis(timer.stop()));
            status.set(STARTED);
            startConsumption(messageReceiver);
            messageReceiver.stop();
        } finally {
            unsetThreadName();
            logger.info("Stopped consumer for subscription {}", subscription.getId());
            stoppedLatch.countDown();
            status.advance(STOPPED);
        }
    }

    private void startConsumption(MessageReceiver messageReceiver) {
        while (isConsuming()) {
            try {
                do {
                    processCommands();
                    status.set(CONSUMING);
                } while (!inflightSemaphore.tryAcquire(500, MILLISECONDS));

                Message message = messageReceiver.next();
                Message convertedMessage = messageConverterResolver.converterFor(message, subscription).convert(message, topic);
                sendMessage(convertedMessage);
            } catch (MessageReceivingTimeoutException messageReceivingTimeoutException) {
                inflightSemaphore.release();
                logger.debug("Timeout while reading message from topic. Trying to read message again", messageReceivingTimeoutException);
            } catch (InterruptedException ex) {
                status.set(STOPPING, MODULE_SHUTDOWN);
                break;
            } catch (Exception e) {
                logger.error("Consumer loop failed for " + getId(), e);
            }
        }
    }

    private void processCommands() {
        if (commands.size() > 0) {
            logger.info("Processing {} commands for subscription {} ", commands.size(), subscription.getId());
            List<Runnable> commands = new ArrayList<>();
            this.commands.drainTo(commands);
            commands.forEach(Runnable::run);
            logger.info("Processed commands for subscription {} ", subscription.getId());
        }
    }

    private MessageReceiver initializeMessageReceiver() {
        try {
            logger.debug("Consumer: preparing message receiver for subscription {}", subscription.getId());
            return messageReceiverFactory.createMessageReceiver(topic, subscription);
        } catch (Exception e) {
            status.set(STOPPED, BROKEN);
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

    @Override
    public void signalStop(Status.ShutdownCause cause) {
        commands.add(() -> {
            status.set(STOPPING, cause);
            logger.info("Stopping consumer for subscription {}", subscription.getId());
            rateLimiter.shutdown();
            sender.shutdown();
            consuming = false;
        });
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

    public void signalUpdate(Subscription newSubscription) {
        commands.add(() -> {
            logger.info("Updating consumer for subscription {}", subscription.getId());
            rateLimiter.updateSubscription(newSubscription);
            sender.updateSubscription(newSubscription);
            messageReceiver.update(newSubscription);
            this.subscription = newSubscription;
        });
    }

    public boolean isConsuming() {
        return consuming;
    }

    @Override
    public Status getStatus() {
        return status.get();
    }
}
