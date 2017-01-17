package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkTracker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class BalancingJob implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(BalancingJob.class);

    private final ConsumerNodesRegistry consumersRegistry;
    private final SubscriptionsCache subscriptionsCache;
    private final SelectiveWorkBalancer workBalancer;
    private final WorkTracker workTracker;
    private final HermesMetrics metrics;
    private final String kafkaCluster;
    private final ScheduledExecutorService executorService;

    private final int intervalSeconds;

    private ScheduledFuture job;

    private final BalancingJobMetrics balancingMetrics = new BalancingJobMetrics();

    public BalancingJob(ConsumerNodesRegistry consumersRegistry,
                        SubscriptionsCache subscriptionsCache,
                        SelectiveWorkBalancer workBalancer,
                        WorkTracker workTracker,
                        HermesMetrics metrics,
                        int intervalSeconds,
                        String kafkaCluster) {
        this.consumersRegistry = consumersRegistry;
        this.subscriptionsCache = subscriptionsCache;
        this.workBalancer = workBalancer;
        this.workTracker = workTracker;
        this.metrics = metrics;
        this.kafkaCluster = kafkaCluster;
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("BalancingExecutor-%d").build();
        this.executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        this.intervalSeconds = intervalSeconds;

        metrics.registerGauge(
                "consumers-workload." + kafkaCluster + ".selective.all-assignments",
                () -> balancingMetrics.allAssignments
        );

        metrics.registerGauge(
                "consumers-workload." + kafkaCluster + ".selective.missing-resources",
                () -> balancingMetrics.missingResources
        );
        metrics.registerGauge(
                "consumers-workload." + kafkaCluster + ".selective.deleted-assignments",
                () -> balancingMetrics.deletedAssignments
        );
        metrics.registerGauge(
                "consumers-workload." + kafkaCluster + ".selective.created-assignments",
                () -> balancingMetrics.createdAssignments
        );
    }

    @Override
    public void run() {
        try {
            consumersRegistry.refresh();
            if (consumersRegistry.isLeader()) {
                try (Timer.Context ctx = metrics.consumersWorkloadRebalanceDurationTimer(kafkaCluster).time()) {
                    logger.info("Initializing workload balance.");

                    WorkBalancingResult work = workBalancer.balance(subscriptionsCache.listActiveSubscriptionNames(),
                            consumersRegistry.list(),
                            workTracker.getAssignments());

                    if (consumersRegistry.isLeader()) {
                        WorkTracker.WorkDistributionChanges changes = workTracker.apply(work.getAssignmentsView());

                        logger.info("Finished workload balance {}, {}", work.toString(), changes.toString());

                        updateMetrics(work, changes);
                    } else {
                        logger.info("Lost leadership before applying changes");
                    }
                }
            } else {
                balancingMetrics.reset();
            }
        } catch (Exception e) {
            logger.error("Caught exception when running balancing job", e);
        }
    }

    public void start() {
        job = executorService.scheduleAtFixedRate(this, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() throws InterruptedException {
        job.cancel(false);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    private void updateMetrics(WorkBalancingResult balancingResult, WorkTracker.WorkDistributionChanges changes) {
        this.balancingMetrics.allAssignments = balancingResult.getAssignmentsView().getAllAssignments().size();
        this.balancingMetrics.missingResources = balancingResult.getMissingResources();
        this.balancingMetrics.createdAssignments = changes.getCreatedAssignmentsCount();
        this.balancingMetrics.deletedAssignments = changes.getDeletedAssignmentsCount();
    }

    private static class BalancingJobMetrics {

        volatile int allAssignments;

        volatile int missingResources;

        volatile int deletedAssignments;

        volatile int createdAssignments;

        void reset() {
            this.allAssignments = 0;
            this.missingResources = 0;
            this.deletedAssignments = 0;
            this.createdAssignments = 0;
        }
    }
}
