package pl.allegro.tech.hermes.consumers.supervisor.monitor;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
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

    private final MonitorMetrics monitorMetrics = new MonitorMetrics();

    public ConsumersRuntimeMonitor(ConsumersSupervisor consumerSupervisor,
                                   SupervisorController workloadSupervisor,
                                   HermesMetrics hermesMetrics,
                                   ConfigFactory configFactory) {
        this.consumerSupervisor = consumerSupervisor;
        this.workloadSupervisor = workloadSupervisor;
        this.scanIntervalSeconds = configFactory.getIntProperty(Configs.CONSUMER_WORKLOAD_MONITOR_SCAN_INTERVAL);

        hermesMetrics.registerGauge("consumers-workload.monitor.running", () -> monitorMetrics.running);
        hermesMetrics.registerGauge("consumers-workload.monitor.assigned", () -> monitorMetrics.assigned);
        hermesMetrics.registerGauge("consumers-workload.monitor.missing", () -> monitorMetrics.missing);
        hermesMetrics.registerGauge("consumers-workload.monitor.oversubscribed", () -> monitorMetrics.oversubscribed);
    }

    public void checkCorrectness() {
        Set<SubscriptionName> assignedSubscriptions = workloadSupervisor.assignedSubscriptions();
        Set<SubscriptionName> runningSubscriptions = consumerSupervisor.runningConsumers();

        monitorMetrics.assigned = assignedSubscriptions.size();
        monitorMetrics.running = runningSubscriptions.size();

        Set<SubscriptionName> missingSubscriptions = missing(assignedSubscriptions, runningSubscriptions);
        monitorMetrics.missing = missingSubscriptions.size();
        for (SubscriptionName subscriptionName : missingSubscriptions) {
            logger.warn("Missing consumer process for subscription: {}", subscriptionName);
        }


        Set<SubscriptionName> oversubscribedSubscriptions = oversubscribed(assignedSubscriptions, runningSubscriptions);
        monitorMetrics.oversubscribed = oversubscribedSubscriptions.size();
        for (SubscriptionName subscriptionName : oversubscribedSubscriptions) {
            logger.warn("Unwanted consumer process for subscription: {}", subscriptionName);
        }

        logger.info(
                "Subscriptions assigned: {}, existing subscriptions: {}, missing: {}, oversubscribed: {}",
                assignedSubscriptions.size(),
                runningSubscriptions.size(),
                missingSubscriptions.size(),
                oversubscribedSubscriptions.size()
        );
    }

    @Override
    public void run() {
        try {
            checkCorrectness();
        } catch (Exception exception) {
            logger.error("Could not check correctness of assignments", exception);
        }
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
