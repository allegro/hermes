package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;

import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_MAXRATE_BALANCE_INTERVAL_SECONDS;

public class MaxRateCalculatorJob implements LeaderLatchListener, Runnable {

    private final int intervalSeconds;
    private final ScheduledExecutorService executorService;
    private final CuratorFramework curator;
    private final LeaderLatch leaderLatch;
    private final MaxRateCalculator maxRateCalculator;

    private ScheduledFuture job;

    public MaxRateCalculatorJob(CuratorFramework curator,
                                ConfigFactory configFactory,
                                ScheduledExecutorService executorService,
                                SubscriptionConsumersCache subscriptionConsumersCache,
                                MaxRateBalancer balancer,
                                MaxRateRegistry maxRateRegistry,
                                String leaderPath,
                                SubscriptionsCache subscriptionsCache,
                                HermesMetrics metrics,
                                Clock clock) {
        this.curator = curator;
        this.intervalSeconds = configFactory.getIntProperty(CONSUMER_MAXRATE_BALANCE_INTERVAL_SECONDS);
        this.executorService = executorService;
        this.maxRateCalculator = new MaxRateCalculator(subscriptionConsumersCache, subscriptionsCache, balancer,
                maxRateRegistry, metrics, clock);
        String consumerId = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_NODE_ID);
        this.leaderLatch = new LeaderLatch(curator, leaderPath, consumerId);
    }

    public void start() {
        try {
            leaderLatch.start();
            leaderLatch.addListener(this);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private boolean hasLeadership() {
        return curator.getZookeeperClient().isConnected() && leaderLatch.hasLeadership();
    }

    @Override
    public void run() {
        if (hasLeadership()) {
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
