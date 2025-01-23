package pl.allegro.tech.hermes.consumers.supervisor.monitor;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkloadSupervisor;

public class ConsumersRuntimeMonitor implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ConsumersRuntimeMonitor.class);

  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("consumer-monitor-%d").build());

  private final Duration scanInterval;

  private final ConsumersSupervisor consumerSupervisor;

  private final WorkloadSupervisor workloadSupervisor;

  private final SubscriptionsCache subscriptionsCache;

  private final MonitorMetrics monitorMetrics = new MonitorMetrics();

  private ScheduledFuture<?> monitoringTask;

  public ConsumersRuntimeMonitor(
      ConsumersSupervisor consumerSupervisor,
      WorkloadSupervisor workloadSupervisor,
      MetricsFacade metrics,
      SubscriptionsCache subscriptionsCache,
      Duration scanInterval) {
    this.consumerSupervisor = consumerSupervisor;
    this.workloadSupervisor = workloadSupervisor;
    this.subscriptionsCache = subscriptionsCache;
    this.scanInterval = scanInterval;

    metrics.workload().registerRunningSubscriptionsGauge(monitorMetrics, mm -> mm.running);
    metrics.workload().registerAssignedSubscriptionsGauge(monitorMetrics, mm -> mm.assigned);
    metrics.workload().registerMissingSubscriptionsGauge(monitorMetrics, mm -> mm.missing);
    metrics.workload().registerOversubscribedGauge(monitorMetrics, mm -> mm.oversubscribed);
  }

  @Override
  public void run() {
    try {
      Set<SubscriptionName> assigned = workloadSupervisor.assignedSubscriptions();
      Set<SubscriptionName> running = consumerSupervisor.runningConsumers();
      Set<SubscriptionName> missing = missing(assigned, running);
      Set<SubscriptionName> oversubscribed = oversubscribed(assigned, running);

      log(assigned, running, missing, oversubscribed);
      updateMetrics(assigned, running, missing, oversubscribed);

      ensureCorrectness(missing, oversubscribed);
    } catch (Exception exception) {
      logger.error("Could not check correctness of assignments", exception);
    }
  }

  public void start() {
    this.monitoringTask =
        executor.scheduleWithFixedDelay(
            this, scanInterval.toSeconds(), scanInterval.toSeconds(), TimeUnit.SECONDS);
  }

  public void shutdown() throws InterruptedException {
    try {
      monitoringTask.cancel(false);
      executor.shutdown();
      executor.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException exception) {
      logger.warn("Got exception when stopping consumers runtime monitor", exception);
    }
  }

  private void ensureCorrectness(
      Set<SubscriptionName> missing, Set<SubscriptionName> oversubscribed) {
    if (!missing.isEmpty() || !oversubscribed.isEmpty()) {
      logger.info(
          "Fixing runtime. Creating {} and killing {} consumers",
          missing.size(),
          oversubscribed.size());
    }
    missing.stream()
        .map(subscriptionsCache::getSubscription)
        .forEach(consumerSupervisor::assignConsumerForSubscription);
    oversubscribed.forEach(consumerSupervisor::deleteConsumerForSubscriptionName);
  }

  private void log(
      Set<SubscriptionName> assigned,
      Set<SubscriptionName> running,
      Set<SubscriptionName> missing,
      Set<SubscriptionName> oversubscribed) {
    for (SubscriptionName subscriptionName : missing) {
      logger.warn("Missing consumer process for subscription: {}", subscriptionName);
    }

    for (SubscriptionName subscriptionName : oversubscribed) {
      logger.warn("Unwanted consumer process for subscription: {}", subscriptionName);
    }

    logger.info(
        "Subscriptions assigned: {}, existing subscriptions: {}, missing: {}, oversubscribed: {}",
        assigned.size(),
        running.size(),
        missing.size(),
        oversubscribed.size());
  }

  private void updateMetrics(
      Set<SubscriptionName> assigned,
      Set<SubscriptionName> running,
      Set<SubscriptionName> missing,
      Set<SubscriptionName> oversubscribed) {
    monitorMetrics.assigned = assigned.size();
    monitorMetrics.running = running.size();
    monitorMetrics.missing = missing.size();
    monitorMetrics.oversubscribed = oversubscribed.size();
  }

  private Set<SubscriptionName> missing(
      Set<SubscriptionName> assignedSubscriptions, Set<SubscriptionName> runningSubscriptions) {
    return Sets.difference(assignedSubscriptions, runningSubscriptions).immutableCopy();
  }

  private Set<SubscriptionName> oversubscribed(
      Set<SubscriptionName> assignedSubscriptions, Set<SubscriptionName> runningSubscriptions) {
    return Sets.difference(runningSubscriptions, assignedSubscriptions).immutableCopy();
  }

  private static class MonitorMetrics {

    volatile int assigned;

    volatile int running;

    volatile int missing;

    volatile int oversubscribed;
  }
}
