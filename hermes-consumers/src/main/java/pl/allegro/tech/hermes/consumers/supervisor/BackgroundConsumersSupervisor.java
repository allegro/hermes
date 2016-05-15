package pl.allegro.tech.hermes.consumers.supervisor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitter;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.consumers.supervisor.background.AssignedConsumers;
import pl.allegro.tech.hermes.consumers.supervisor.background.ConsumerSupervisorProcess;
import pl.allegro.tech.hermes.consumers.supervisor.background.Retransmitter;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.api.Subscription.State.ACTIVE;
import static pl.allegro.tech.hermes.api.Subscription.State.PENDING;

public class BackgroundConsumersSupervisor implements ConsumersSupervisor {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundConsumersSupervisor.class);

    private ConsumerSupervisorProcess backgroundProcess;
    private ConsumerFactory consumerFactory;
    private HermesMetrics hermesMetrics;
    private UndeliveredMessageLogPersister undeliveredMessageLogPersister;
    private ConfigFactory configs;
    private OffsetCommitter offsetCommitter;
    private SubscriptionRepository subscriptionRepository;

    private final ScheduledExecutorService scheduledExecutor;

    private final AssignedConsumers assignedConsumers;

    public BackgroundConsumersSupervisor(ConfigFactory configFactory,
                                         SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator,
                                         ConsumersExecutorService executor,
                                         ConsumerFactory consumerFactory,
                                         List<MessageCommitter> messageCommitters,
                                         List<OffsetsStorage> offsetsStorages,
                                         HermesMetrics hermesMetrics,
                                         UndeliveredMessageLogPersister undeliveredMessageLogPersister,
                                         SubscriptionRepository subscriptionRepository,
                                         Clock clock) {
        this.consumerFactory = consumerFactory;
        this.hermesMetrics = hermesMetrics;
        this.undeliveredMessageLogPersister = undeliveredMessageLogPersister;
        this.subscriptionRepository = subscriptionRepository;
        this.assignedConsumers = new AssignedConsumers();
        this.configs = configFactory;
        this.offsetCommitter = new OffsetCommitter(() -> assignedConsumers, messageCommitters, configFactory);
        Retransmitter retransmitter = new Retransmitter(subscriptionOffsetChangeIndicator, offsetsStorages, configFactory);
        this.backgroundProcess = new ConsumerSupervisorProcess(assignedConsumers, executor, retransmitter, clock, configFactory);
        this.scheduledExecutor = createExecutorForSupervision();
    }

    private ScheduledExecutorService createExecutorForSupervision() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("BackgroundConsumersSupervisor-%d")
                .setUncaughtExceptionHandler((t, e) -> logger.error("Exception from supervisor with name {}", t.getName(), e)).build();
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    @Override
    public void assignConsumerForSubscription(Subscription subscription) {
        logger.info("Creating consumer for {}", subscription.getId());
        try {
            Consumer consumer = consumerFactory.createConsumer(subscription);
            logger.info("Created consumer for {}", subscription.getId());
            assignedConsumers.add(consumer);
            if (subscription.getState() == PENDING) {
                subscriptionRepository.updateSubscriptionState(subscription.getTopicName(), subscription.getName(), ACTIVE);
            }
            logger.info("Consumer for {} was added for execution", subscription.getId());
        } catch (Exception ex) {
            logger.info("Failed to create consumer for subscription {} ", subscription.getId(), ex);
        }
    }

    @Override
    public void deleteConsumerForSubscriptionName(SubscriptionName subscription) {
        assignedConsumers.stop(subscription);
    }

    @Override
    public void updateSubscription(Subscription subscription) {
        assignedConsumers.update(subscription);
    }

    @Override
    public void start() {
        scheduledExecutor.scheduleAtFixedRate(
                backgroundProcess,
                configs.getIntProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_INTERVAL),
                configs.getIntProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_INTERVAL),
                TimeUnit.SECONDS);
        offsetCommitter.start();
        undeliveredMessageLogPersister.start();
    }

    @Override
    public void shutdown() {
        backgroundProcess.shutdown();
        scheduledExecutor.shutdown();
    }

    @Override
    public void retransmit(SubscriptionName subscription) {
        assignedConsumers.retransmit(subscription);
    }

    @Override
    public void restartConsumer(SubscriptionName subscription) {
        assignedConsumers.restart(subscription);
    }
}
