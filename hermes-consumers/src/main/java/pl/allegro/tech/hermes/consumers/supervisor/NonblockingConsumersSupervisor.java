package pl.allegro.tech.hermes.consumers.supervisor;

import static pl.allegro.tech.hermes.api.Subscription.State.ACTIVE;
import static pl.allegro.tech.hermes.api.Subscription.State.PENDING;
import static pl.allegro.tech.hermes.consumers.health.Checks.SUBSCRIPTIONS;
import static pl.allegro.tech.hermes.consumers.health.Checks.SUBSCRIPTIONS_COUNT;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.CommonConsumerParameters;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.health.ConsumerMonitor;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.consumers.supervisor.process.ConsumerProcessFactory;
import pl.allegro.tech.hermes.consumers.supervisor.process.ConsumerProcessSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.process.Retransmitter;
import pl.allegro.tech.hermes.consumers.supervisor.process.Signal;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

public class NonblockingConsumersSupervisor implements ConsumersSupervisor {
  private static final Logger logger =
      LoggerFactory.getLogger(NonblockingConsumersSupervisor.class);

  private final ConsumerProcessSupervisor backgroundProcess;
  private final UndeliveredMessageLogPersister undeliveredMessageLogPersister;
  private final Duration backgroundSupervisorInterval;
  private final SubscriptionRepository subscriptionRepository;

  private final ScheduledExecutorService scheduledExecutor;

  public NonblockingConsumersSupervisor(
      CommonConsumerParameters commonConsumerParameters,
      ConsumersExecutorService executor,
      ConsumerFactory consumerFactory,
      ConsumerPartitionAssignmentState consumerPartitionAssignmentState,
      Retransmitter retransmitter,
      UndeliveredMessageLogPersister undeliveredMessageLogPersister,
      SubscriptionRepository subscriptionRepository,
      MetricsFacade metrics,
      ConsumerMonitor monitor,
      Clock clock) {
    this.undeliveredMessageLogPersister = undeliveredMessageLogPersister;
    this.subscriptionRepository = subscriptionRepository;
    this.backgroundSupervisorInterval =
        commonConsumerParameters.getBackgroundSupervisor().getInterval();
    this.backgroundProcess =
        new ConsumerProcessSupervisor(
            executor,
            clock,
            metrics,
            new ConsumerProcessFactory(
                retransmitter,
                consumerFactory,
                commonConsumerParameters.getBackgroundSupervisor().getUnhealthyAfter(),
                clock),
            commonConsumerParameters.getSignalProcessingQueueSize(),
            commonConsumerParameters.getBackgroundSupervisor().getKillAfter());
    this.scheduledExecutor = createExecutorForSupervision();
    monitor.register(SUBSCRIPTIONS, backgroundProcess::runningSubscriptionsStatus);
    monitor.register(SUBSCRIPTIONS_COUNT, backgroundProcess::countRunningProcesses);
  }

  private ScheduledExecutorService createExecutorForSupervision() {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder()
            .setNameFormat("NonblockingConsumersSupervisor-%d")
            .setUncaughtExceptionHandler(
                (t, e) -> logger.error("Exception from supervisor with name {}", t.getName(), e))
            .build();
    return Executors.newSingleThreadScheduledExecutor(threadFactory);
  }

  @Override
  public void assignConsumerForSubscription(Subscription subscription) {
    try {
      Signal start =
          Signal.of(Signal.SignalType.START, subscription.getQualifiedName(), subscription);

      backgroundProcess.accept(start);

      if (subscription.getState() == PENDING) {
        subscriptionRepository.updateSubscriptionState(
            subscription.getTopicName(), subscription.getName(), ACTIVE);
      }
    } catch (RuntimeException e) {
      logger.error(
          "Error during assigning subscription {} to consumer", subscription.getQualifiedName(), e);
    }
  }

  @Override
  public void deleteConsumerForSubscriptionName(SubscriptionName subscription) {
    Signal stop = Signal.of(Signal.SignalType.STOP, subscription);
    logger.info("Deleting consumer for {}. {}", subscription, stop.getLogWithIdAndType());
    backgroundProcess.accept(stop);
  }

  @Override
  public void updateTopic(Subscription subscription, Topic topic) {
    backgroundProcess.accept(
        Signal.of(Signal.SignalType.UPDATE_TOPIC, subscription.getQualifiedName(), topic));
  }

  @Override
  public void updateSubscription(Subscription subscription) {
    backgroundProcess.accept(
        Signal.of(
            Signal.SignalType.UPDATE_SUBSCRIPTION, subscription.getQualifiedName(), subscription));
  }

  @Override
  public void retransmit(SubscriptionName subscription) {
    backgroundProcess.accept(Signal.of(Signal.SignalType.RETRANSMIT, subscription));
  }

  @Override
  public Set<SubscriptionName> runningConsumers() {
    return backgroundProcess.existingConsumers();
  }

  @Override
  public void start() {
    scheduledExecutor.scheduleAtFixedRate(
        backgroundProcess,
        backgroundSupervisorInterval.toMillis(),
        backgroundSupervisorInterval.toMillis(),
        TimeUnit.MILLISECONDS);
    undeliveredMessageLogPersister.start();
  }

  @Override
  public void shutdown() {
    backgroundProcess.shutdown();
    scheduledExecutor.shutdown();
    undeliveredMessageLogPersister.shutdown();
  }
}
