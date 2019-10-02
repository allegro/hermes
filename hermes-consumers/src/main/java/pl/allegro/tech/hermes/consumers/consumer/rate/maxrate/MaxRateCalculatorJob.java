package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ClusterAssignmentCache;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_MAXRATE_BALANCE_INTERVAL_SECONDS;

class MaxRateCalculatorJob implements LeaderLatchListener, Runnable {

    private final int intervalSeconds;
    private final ScheduledExecutorService executorService;
    private final MaxRateCalculator maxRateCalculator;
    private final ConsumerNodesRegistry consumerNodesRegistry;

    private ScheduledFuture job;

    MaxRateCalculatorJob(ConfigFactory configFactory,
                         ClusterAssignmentCache clusterAssignmentCache,
                         ConsumerNodesRegistry consumerNodesRegistry,
                         MaxRateBalancer balancer,
                         MaxRateRegistry maxRateRegistry,
                         SubscriptionsCache subscriptionsCache,
                         HermesMetrics metrics,
                         Clock clock) {
        this.consumerNodesRegistry = consumerNodesRegistry;
        this.intervalSeconds = configFactory.getIntProperty(CONSUMER_MAXRATE_BALANCE_INTERVAL_SECONDS);
        this.executorService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("max-rate-calculator-%d").build());
        this.maxRateCalculator = new MaxRateCalculator(
                clusterAssignmentCache, subscriptionsCache, balancer, maxRateRegistry, metrics, clock
        );
    }

    public void start() {
        consumerNodesRegistry.addLeaderLatchListener(this);
        startJobIfAlreadyBeingLeader();
    }

    private void startJobIfAlreadyBeingLeader() {
        if (consumerNodesRegistry.isLeader()) {
            isLeader();
        }
    }

    public void stop() throws InterruptedException {
        consumerNodesRegistry.removeLeaderLatchListener(this);

        if (job != null) {
            job.cancel(false);
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        if (consumerNodesRegistry.isLeader()) {
            maxRateCalculator.calculate();
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
