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
import pl.allegro.tech.hermes.consumers.consumer.offset.BetterOffsetCommiter;
import pl.allegro.tech.hermes.consumers.consumer.offset.BetterOffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.consumers.supervisor.process.ConsumerProcess;
import pl.allegro.tech.hermes.consumers.supervisor.process.ConsumerProcessSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.process.Retransmitter;
import pl.allegro.tech.hermes.consumers.supervisor.process.Signal;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

import javax.inject.Inject;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.api.Subscription.State.ACTIVE;
import static pl.allegro.tech.hermes.api.Subscription.State.PENDING;

/**
 * Design doc:
 * * background consumers supervisor runs ConsumerProcessSupervisor periodically
 * * all work that needs to be done for a Consumer is pushed to ConsumerProcessSupervisor queue (MPSC)
 * * work from the queue is executed
 * * ConsumerProcessSupervisor also checks liveliness of a ConsumerProcess
 * * in case Consumer is unhealthy, the RESTART_KILL signal is sent to queue and executed next time
 *
 * OffsetCommiting -> Consumers push offsets to commit (in any order) to shared MPSC queue, it is drained periodically
 * and whole magic happens in single thread. No locks!
 */
public class ProcessConsumersSupervisor implements ConsumersSupervisor {

    private static final Logger logger = LoggerFactory.getLogger(ProcessConsumersSupervisor.class);

    private ConsumerProcessSupervisor backgroundProcess;
    private ConsumerFactory consumerFactory;
    private UndeliveredMessageLogPersister undeliveredMessageLogPersister;
    private ConfigFactory configs;
    private BetterOffsetCommiter offsetCommitter;
    private SubscriptionRepository subscriptionRepository;

    private final ScheduledExecutorService scheduledExecutor;

    @Inject
    public ProcessConsumersSupervisor(ConfigFactory configFactory,
                                      ConsumersExecutorService executor,
                                      ConsumerFactory consumerFactory,
                                      List<MessageCommitter> messageCommitters,
                                      BetterOffsetQueue offsetQueue,
                                      Retransmitter retransmitter,
                                      UndeliveredMessageLogPersister undeliveredMessageLogPersister,
                                      SubscriptionRepository subscriptionRepository,
                                      Clock clock) {
        this.consumerFactory = consumerFactory;
        this.undeliveredMessageLogPersister = undeliveredMessageLogPersister;
        this.subscriptionRepository = subscriptionRepository;
        this.configs = configFactory;
        this.offsetCommitter = new BetterOffsetCommiter(offsetQueue, messageCommitters, configFactory.getIntProperty(Configs.CONSUMER_COMMIT_OFFSET_PERIOD));
        this.backgroundProcess = new ConsumerProcessSupervisor(executor, retransmitter, clock, configFactory);
        this.scheduledExecutor = createExecutorForSupervision();
    }

    private ScheduledExecutorService createExecutorForSupervision() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("ProcessConsumersSupervisor-%d")
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
            logger.info("Failed to create consumer for subscription {}", subscription.getId(), ex);
        }
    }

    @Override
    public void deleteConsumerForSubscriptionName(SubscriptionName subscription) {
        backgroundProcess.accept(Signal.of(Signal.SignalType.STOP, subscription));
    }

    @Override
    public void updateSubscription(Subscription subscription) {
        backgroundProcess.accept(Signal.of(Signal.SignalType.UPDATE, subscription.toSubscriptionName(), subscription));
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
