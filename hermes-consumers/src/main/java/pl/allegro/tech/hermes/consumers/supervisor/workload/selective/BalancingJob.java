package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

import com.codahale.metrics.Timer;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkTracker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BalancingJob implements LeaderLatchListener, Runnable {

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
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.intervalSeconds = intervalSeconds;
    }

    @Override
    public void run() {
        try {
            if (consumersRegistry.isLeader()) {
                try (Timer.Context ctx = metrics.consumersWorkloadRebalanceDurationTimer(kafkaCluster).time()) {
                    logger.info("Initializing workload balance.");
                    WorkBalancingResult work = workBalancer.balance(subscriptionsCache.listActiveSubscriptionNames(),
                            consumersRegistry.list(),
                            workTracker.getAssignments());
                    WorkTracker.WorkDistributionChanges changes = workTracker.apply(work.getAssignmentsView());
                    logger.info("Finished workload balance {}, {}", work.toString(), changes.toString());
                    metrics.reportConsumersWorkloadStats(kafkaCluster, work.getMissingResources(), changes.getDeletedAssignmentsCount(), changes.getCreatedAssignmentsCount());
                }
            }
        }
        catch(Exception e) {
            logger.error("Caught exception when running balancing job", e);
        }
    }

    @Override
    public void isLeader() {
        job = executorService.scheduleAtFixedRate(this, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void notLeader() {
        job.cancel(false);
    }
}
