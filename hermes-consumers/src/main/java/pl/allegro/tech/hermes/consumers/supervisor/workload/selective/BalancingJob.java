package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentView;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkTracker;
import pl.allegro.tech.hermes.consumers.supervisor.workload.constraints.ConsumersWorkloadConstraints;
import pl.allegro.tech.hermes.consumers.supervisor.workload.constraints.WorkloadConstraints;
import pl.allegro.tech.hermes.consumers.supervisor.workload.constraints.WorkloadConstraintsRepository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_MAX_SUBSCRIPTIONS_PER_CONSUMER;

public class BalancingJob implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(BalancingJob.class);

    private final ConsumerNodesRegistry consumersRegistry;
    private final ConfigFactory configFactory;
    private final SubscriptionsCache subscriptionsCache;
    private final SelectiveWorkBalancer workBalancer;
    private final WorkTracker workTracker;
    private final HermesMetrics metrics;
    private final String kafkaCluster;
    private final WorkloadConstraintsRepository workloadConstraintsRepository;
    private final ScheduledExecutorService executorService;

    private final int intervalSeconds;

    private ScheduledFuture job;

    private final BalancingJobMetrics balancingMetrics = new BalancingJobMetrics();

    BalancingJob(ConsumerNodesRegistry consumersRegistry,
                 ConfigFactory configFactory,
                 SubscriptionsCache subscriptionsCache,
                 SelectiveWorkBalancer workBalancer,
                 WorkTracker workTracker,
                 HermesMetrics metrics,
                 int intervalSeconds,
                 String kafkaCluster,
                 WorkloadConstraintsRepository workloadConstraintsRepository) {
        this.consumersRegistry = consumersRegistry;
        this.configFactory = configFactory;
        this.subscriptionsCache = subscriptionsCache;
        this.workBalancer = workBalancer;
        this.workTracker = workTracker;
        this.metrics = metrics;
        this.kafkaCluster = kafkaCluster;
        this.workloadConstraintsRepository = workloadConstraintsRepository;
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("BalancingExecutor-%d").build();
        this.executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        this.intervalSeconds = intervalSeconds;

        metrics.registerGauge(
                gaugeName(kafkaCluster, "selective.all-assignments"),
                () -> balancingMetrics.allAssignments
        );

        metrics.registerGauge(
                gaugeName(kafkaCluster, "selective.missing-resources"),
                () -> balancingMetrics.missingResources
        );
        metrics.registerGauge(
                gaugeName(kafkaCluster, ".selective.deleted-assignments"),
                () -> balancingMetrics.deletedAssignments
        );
        metrics.registerGauge(
                gaugeName(kafkaCluster, ".selective.created-assignments"),
                () -> balancingMetrics.createdAssignments
        );
    }

    private String gaugeName(String kafkaCluster, String name) {
        return "consumers-workload." + kafkaCluster + "." + name;
    }

    @Override
    public void run() {
        try {
            consumersRegistry.refresh();
            if (consumersRegistry.isLeader() && workTracker.isReady()) {
                try (Timer.Context ctx = metrics.consumersWorkloadRebalanceDurationTimer(kafkaCluster).time()) {
                    logger.info("Initializing workload balance.");

                    SubscriptionAssignmentView initialState = workTracker.getAssignments();

                    ConsumersWorkloadConstraints constraints = workloadConstraintsRepository.getConsumersWorkloadConstraints();
                    WorkloadConstraints workloadConstraints = new WorkloadConstraints(
                            constraints.getSubscriptionConstraints(),
                            constraints.getTopicConstraints(),
                            configFactory.getIntProperty(CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION),
                            configFactory.getIntProperty(CONSUMER_WORKLOAD_MAX_SUBSCRIPTIONS_PER_CONSUMER),
                            consumersRegistry.list().size());

                    WorkBalancingResult work = workBalancer.balance(
                            subscriptionsCache.listActiveSubscriptionNames(),
                            consumersRegistry.list(),
                            initialState,
                            workloadConstraints);

                    if (consumersRegistry.isLeader()) {
                        logger.info("Applying workload balance changes {}", work.toString());
                        WorkTracker.WorkDistributionChanges changes =
                                workTracker.apply(initialState, work.getAssignmentsView());

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
