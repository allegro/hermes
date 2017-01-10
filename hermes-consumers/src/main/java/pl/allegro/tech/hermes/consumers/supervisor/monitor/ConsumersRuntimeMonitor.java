package pl.allegro.tech.hermes.consumers.supervisor.monitor;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConsumersRuntimeMonitor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ConsumersRuntimeMonitor.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("consumer-monitor-%d").build()
    );

    private final int scanIntervalSeconds;

    private final ConsumersSupervisor consumerSupervisor;

    private final SupervisorController workloadSupervisor;

    private final SubscriptionsCache subscriptionsCache;

    private final MonitorMetrics monitorMetrics = new MonitorMetrics();

    public ConsumersRuntimeMonitor(ConsumersSupervisor consumerSupervisor,
                                   SupervisorController workloadSupervisor,
                                   HermesMetrics hermesMetrics,
                                   SubscriptionsCache subscriptionsCache, ConfigFactory configFactory) {
        this.consumerSupervisor = consumerSupervisor;
        this.workloadSupervisor = workloadSupervisor;
        this.subscriptionsCache = subscriptionsCache;
        this.scanIntervalSeconds = configFactory.getIntProperty(Configs.CONSUMER_WORKLOAD_MONITOR_SCAN_INTERVAL);

        hermesMetrics.registerGauge("consumers-workload.monitor.running", () -> monitorMetrics.running);
        hermesMetrics.registerGauge("consumers-workload.monitor.assigned", () -> monitorMetrics.assigned);
        hermesMetrics.registerGauge("consumers-workload.monitor.missing", () -> monitorMetrics.missing);
        hermesMetrics.registerGauge("consumers-workload.monitor.oversubscribed", () -> monitorMetrics.oversubscribed);
    }

    public void log(Set<SubscriptionName> assigned,
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
                oversubscribed.size()
        );
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

    private void ensureCorrectness(Set<SubscriptionName> missing, Set<SubscriptionName> oversubscribed) {
        if (!missing.isEmpty() || !oversubscribed.isEmpty()) {
            logger.info("Fixing runtime. Creating {} and killing {} consumers", missing.size(), oversubscribed.size());
        }
        missing.stream()
                .map(subscriptionsCache::getSubscription)
                .forEach(consumerSupervisor::assignConsumerForSubscription);
        oversubscribed.forEach(consumerSupervisor::deleteConsumerForSubscriptionName);
    }

    private void updateMetrics(Set<SubscriptionName> assigned,
                               Set<SubscriptionName> running,
                               Set<SubscriptionName> missing,
                               Set<SubscriptionName> oversubscribed) {
        monitorMetrics.assigned = assigned.size();
        monitorMetrics.running = running.size();
        monitorMetrics.missing = missing.size();
        monitorMetrics.oversubscribed = oversubscribed.size();
    }

    private Set<SubscriptionName> missing(Set<SubscriptionName> assignedSubscriptions,
                                          Set<SubscriptionName> runningSubscriptions) {
        return Sets.difference(assignedSubscriptions, runningSubscriptions).immutableCopy();
    }

    private Set<SubscriptionName> oversubscribed(Set<SubscriptionName> assignedSubscriptions,
                                                 Set<SubscriptionName> runningSubscriptions) {
        return Sets.difference(runningSubscriptions, assignedSubscriptions).immutableCopy();
    }

    public void start() {
        executor.scheduleWithFixedDelay(this, scanIntervalSeconds, scanIntervalSeconds, TimeUnit.SECONDS);
    }

    public void shutdown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    private static class MonitorMetrics {

        volatile int assigned;

        volatile int running;

        volatile int missing;

        volatile int oversubscribed;

    }
}
