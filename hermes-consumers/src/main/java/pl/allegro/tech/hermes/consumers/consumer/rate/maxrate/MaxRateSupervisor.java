package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import java.time.Clock;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MaxRateSupervisor implements Runnable {

    private final Set<MaxRateProvider> providers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ConfigFactory configFactory;
    private final CuratorFramework curator;
    private final SubscriptionConsumersCache subscriptionConsumersCache;
    private final MaxRateRegistry maxRateRegistry;
    private final SubscriptionsCache subscriptionsCache;
    private final ZookeeperPaths zookeeperPaths;
    private final HermesMetrics metrics;
    private final Clock clock;

    @Inject
    public MaxRateSupervisor(ConfigFactory configFactory,
                             CuratorFramework curator,
                             SubscriptionConsumersCache subscriptionConsumersCache,
                             MaxRateRegistry maxRateRegistry,
                             SubscriptionsCache subscriptionsCache,
                             ZookeeperPaths zookeeperPaths,
                             HermesMetrics metrics,
                             Clock clock) {
        this.configFactory = configFactory;
        this.curator = curator;
        this.subscriptionConsumersCache = subscriptionConsumersCache;
        this.maxRateRegistry = maxRateRegistry;
        this.subscriptionsCache = subscriptionsCache;
        this.zookeeperPaths = zookeeperPaths;
        this.metrics = metrics;
        this.clock = clock;
    }

    public void start() throws Exception {

        MaxRateBalancer balancer = new MaxRateBalancer(
                configFactory.getDoubleProperty(Configs.CONSUMER_MAXRATE_BUSY_TOLERANCE),
                configFactory.getDoubleProperty(Configs.CONSUMER_MAXRATE_MIN_MAX_RATE),
                configFactory.getDoubleProperty(Configs.CONSUMER_MAXRATE_MIN_ALLOWED_CHANGE_PERCENT));

        subscriptionConsumersCache.start();
        new MaxRateCalculatorJob(
                curator,
                configFactory,
                Executors.newSingleThreadScheduledExecutor(),
                subscriptionConsumersCache,
                balancer,
                maxRateRegistry,
                zookeeperPaths.maxRateLeaderPath(),
                subscriptionsCache,
                metrics,
                clock
        ).start();

        int selfUpdateInterval = configFactory.getIntProperty(Configs.CONSUMER_MAXRATE_UPDATE_INTERVAL_SECONDS);
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this, selfUpdateInterval, selfUpdateInterval, TimeUnit.SECONDS);
    }

    public void shutdown() throws InterruptedException {
        subscriptionConsumersCache.stop();
    }

    @Override
    public void run() {
        providers.forEach(MaxRateProvider::tickForHistory);
    }

    public void register(MaxRateProvider maxRateProvider) {
        providers.add(maxRateProvider);
    }
}
