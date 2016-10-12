package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MaxRateCalculatorJob implements LeaderLatchListener, Runnable {

    private final int intervalSeconds;
    private final ScheduledExecutorService executorService;
    private final CuratorFramework curator;
    private final LeaderLatch leaderLatch;
    private final MaxRateCalculator maxRateCalculator;

    private ScheduledFuture job;

    public MaxRateCalculatorJob(CuratorFramework curator,
                                String consumerNodeId,
                                int intervalSeconds,
                                ScheduledExecutorService executorService,
                                SubscriptionConsumersCache subscriptionConsumersCache,
                                MaxRateBalancer balancer,
                                MaxRateRegistry maxRateRegistry,
                                String leaderPath, SubscriptionsCache subscriptionsCache) {
        this.curator = curator;
        this.intervalSeconds = intervalSeconds;
        this.executorService = executorService;
        this.maxRateCalculator = new MaxRateCalculator(subscriptionConsumersCache, subscriptionsCache, balancer, maxRateRegistry);
        this.leaderLatch = new LeaderLatch(curator, leaderPath, consumerNodeId);
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
