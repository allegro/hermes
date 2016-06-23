package pl.allegro.tech.hermes.consumers.supervisor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitter;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.consumers.supervisor.process.ConsumerProcessSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.process.Retransmitter;
import pl.allegro.tech.hermes.consumers.supervisor.process.Signal;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

import javax.inject.Inject;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.api.Subscription.State.ACTIVE;
import static pl.allegro.tech.hermes.api.Subscription.State.PENDING;

public class NonblockingConsumersSupervisor implements ConsumersSupervisor {

    private static final Logger logger = LoggerFactory.getLogger(NonblockingConsumersSupervisor.class);

    private final ConsumerProcessSupervisor backgroundProcess;
    private final ConsumerFactory consumerFactory;
    private final UndeliveredMessageLogPersister undeliveredMessageLogPersister;
    private final ConfigFactory configs;
    private final OffsetCommitter offsetCommitter;
    private final SubscriptionRepository subscriptionRepository;

    private final ScheduledExecutorService scheduledExecutor;

    @Inject
    public NonblockingConsumersSupervisor(ConfigFactory configFactory,
                                          ConsumersExecutorService executor,
                                          ConsumerFactory consumerFactory,
                                          List<MessageCommitter> messageCommitters,
                                          OffsetQueue offsetQueue,
                                          Retransmitter retransmitter,
                                          UndeliveredMessageLogPersister undeliveredMessageLogPersister,
                                          SubscriptionRepository subscriptionRepository,
                                          Clock clock) {
        this.consumerFactory = consumerFactory;
        this.undeliveredMessageLogPersister = undeliveredMessageLogPersister;
        this.subscriptionRepository = subscriptionRepository;
        this.configs = configFactory;
        this.offsetCommitter = new OffsetCommitter(offsetQueue, messageCommitters, configFactory.getIntProperty(Configs.CONSUMER_COMMIT_OFFSET_PERIOD));
        this.backgroundProcess = new ConsumerProcessSupervisor(executor, retransmitter, clock, configFactory);
        this.scheduledExecutor = createExecutorForSupervision();
    }

    private ScheduledExecutorService createExecutorForSupervision() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("NonblockingConsumersSupervisor-%d")
                .setUncaughtExceptionHandler((t, e) -> logger.error("Exception from supervisor with name {}", t.getName(), e)).build();
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    @Override
    public void assignConsumerForSubscription(Subscription subscription) {
        logger.info("Creating consumer for {}", subscription.getId());
        try {
            Consumer consumer = consumerFactory.createConsumer(subscription);
            logger.info("Created consumer for {}", subscription.getId());

            backgroundProcess.accept(Signal.of(Signal.SignalType.START, subscription.toSubscriptionName(), consumer));

            if (subscription.getState() == PENDING) {
                subscriptionRepository.updateSubscriptionState(subscription.getTopicName(), subscription.getName(), ACTIVE);
            }
            logger.info("Consumer for {} was added for execution", subscription.getId());
        } catch (Exception ex) {
            logger.error("Failed to create consumer for subscription {}", subscription.getId(), ex);
        }
    }

    @Override
    public void deleteConsumerForSubscriptionName(SubscriptionName subscription) {
        backgroundProcess.accept(Signal.of(Signal.SignalType.STOP, subscription));
    }

    @Override
    public void updateTopic(Subscription subscription, Topic topic) {
        backgroundProcess.accept(Signal.of(Signal.SignalType.UPDATE_TOPIC, subscription.toSubscriptionName(), topic));
    }

    @Override
    public void updateSubscription(Subscription subscription) {
        backgroundProcess.accept(Signal.of(Signal.SignalType.UPDATE_SUBSCRIPTION, subscription.toSubscriptionName(), subscription));
    }

    @Override
    public void retransmit(SubscriptionName subscription) {
        backgroundProcess.accept(Signal.of(Signal.SignalType.RETRANSMIT, subscription));
    }

    @Override
    public void restartConsumer(SubscriptionName subscription) {
        backgroundProcess.accept(Signal.of(Signal.SignalType.RESTART, subscription));
    }

    @Override
    public void start() {
        scheduledExecutor.scheduleAtFixedRate(
                backgroundProcess,
                configs.getIntProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_INTERVAL),
                configs.getIntProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_INTERVAL),
                TimeUnit.MILLISECONDS);
        offsetCommitter.start();
        undeliveredMessageLogPersister.start();
    }

    @Override
    public void shutdown() {
        backgroundProcess.shutdown();
        scheduledExecutor.shutdown();
        offsetCommitter.shutdown();
        undeliveredMessageLogPersister.shutdown();
    }
}
