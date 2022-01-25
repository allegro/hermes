package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ClusterAssignmentCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import java.time.Clock;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MaxRateSupervisor implements Runnable {

    private final Set<NegotiatedMaxRateProvider> providers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ConfigFactory configFactory;
    private final ScheduledExecutorService selfUpdateExecutor;
    private final MaxRateCalculatorJob calculatorJob;
    private final MaxRateRegistry maxRateRegistry;
    private ScheduledFuture<?> updateJob;

    @Inject
    public MaxRateSupervisor(ConfigFactory configFactory,
                             ClusterAssignmentCache clusterAssignmentCache,
                             MaxRateRegistry maxRateRegistry,
                             ConsumerNodesRegistry consumerNodesRegistry,
                             SubscriptionsCache subscriptionsCache,
                             ZookeeperPaths zookeeperPaths,
                             HermesMetrics metrics,
                             Clock clock) {
        this.configFactory = configFactory;
        this.maxRateRegistry = maxRateRegistry;

        this.selfUpdateExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("max-rate-provider-%d").build()
        );

        MaxRateBalancer balancer = new MaxRateBalancer(
                configFactory.getDoubleProperty(Configs.CONSUMER_MAXRATE_BUSY_TOLERANCE),
                configFactory.getDoubleProperty(Configs.CONSUMER_MAXRATE_MIN_MAX_RATE),
                configFactory.getDoubleProperty(Configs.CONSUMER_MAXRATE_MIN_ALLOWED_CHANGE_PERCENT));

        this.calculatorJob = new MaxRateCalculatorJob(
                configFactory,
                clusterAssignmentCache,
                consumerNodesRegistry,
                balancer,
                maxRateRegistry,
                subscriptionsCache,
                metrics,
                clock
        );
    }

    public void start() throws Exception {
        maxRateRegistry.start();
        calculatorJob.start();
        updateJob = startSelfUpdate();
    }

    public void stop() throws Exception {//TODO: use @PreDestroy?
        maxRateRegistry.stop();
        calculatorJob.stop();
        if (updateJob != null) {
            updateJob.cancel(false);
        }
        selfUpdateExecutor.shutdown();
        selfUpdateExecutor.awaitTermination(10, TimeUnit.SECONDS);
    }

    private ScheduledFuture<?> startSelfUpdate() {
        int selfUpdateInterval = configFactory.getIntProperty(Configs.CONSUMER_MAXRATE_UPDATE_INTERVAL_SECONDS);
        return selfUpdateExecutor.scheduleAtFixedRate(
                this, 0, selfUpdateInterval, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        providers.forEach(NegotiatedMaxRateProvider::tickForHistory);
        maxRateRegistry.onAfterWriteRateHistories();
    }

    public void register(NegotiatedMaxRateProvider maxRateProvider) {
        providers.add(maxRateProvider);
    }

    public void unregister(NegotiatedMaxRateProvider maxRateProvider) {
        providers.remove(maxRateProvider);
    }
}
