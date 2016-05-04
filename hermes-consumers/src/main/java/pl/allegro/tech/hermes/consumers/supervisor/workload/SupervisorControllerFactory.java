package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.LegacyConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerWorkloadAlgorithm.UnsupportedConsumerWorkloadAlgorithm;
import pl.allegro.tech.hermes.consumers.supervisor.workload.mirror.LegacyMirroringSupervisorController;
import pl.allegro.tech.hermes.consumers.supervisor.workload.mirror.MirroringSupervisorController;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveSupervisorController;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_ALGORITHM;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_ASSIGNMENT_PROCESSING_THREAD_POOL_SIZE;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerWorkloadAlgorithm.LEGACY_MIRROR;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerWorkloadAlgorithm.MIRROR;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerWorkloadAlgorithm.SELECTIVE;

public class SupervisorControllerFactory implements Factory<SupervisorController> {
    private final ConfigFactory configs;
    private final Map<String, Provider<SupervisorController>> availableImplementations;

    @Inject
    public SupervisorControllerFactory(@Named(CuratorType.HERMES) CuratorFramework curator,
                                       SubscriptionsCache subscriptionsCache,
                                       WorkTracker workTracker,
                                       ConsumersSupervisor supervisor,
                                       ZookeeperAdminCache adminCache,
                                       HermesMetrics metrics,
                                       ConfigFactory configs) {
        this.configs = configs;
        this.availableImplementations = ImmutableMap.of(
                LEGACY_MIRROR, () -> new LegacyMirroringSupervisorController((LegacyConsumersSupervisor) supervisor, subscriptionsCache, adminCache, configs),
                MIRROR, () -> new MirroringSupervisorController(supervisor, subscriptionsCache, workTracker, adminCache, configs),
                SELECTIVE, () -> new SelectiveSupervisorController(supervisor, subscriptionsCache, workTracker,
                                                                  createConsumersRegistry(configs, curator), adminCache,
                                                                  getAssignmentExecutor(configs),
                                                                  configs, metrics));
    }

    private ExecutorService getAssignmentExecutor(ConfigFactory configs) {
        Logger logger = LoggerFactory.getLogger(SupervisorControllerFactory.class);
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("AssignmentExecutor-%d")
                .setUncaughtExceptionHandler((t, e) -> logger.error("AssignmentExecutor failed {}", t.getName(), e)).build();
        return newFixedThreadPool(configs.getIntProperty(CONSUMER_WORKLOAD_ASSIGNMENT_PROCESSING_THREAD_POOL_SIZE), threadFactory);
    }

    private static ConsumerNodesRegistry createConsumersRegistry(ConfigFactory configs, CuratorFramework curator) {
        return new ConsumerNodesRegistry(curator,
                newSingleThreadExecutor(),
                new ZookeeperPaths(configs.getStringProperty(Configs.ZOOKEEPER_ROOT)).consumersRegistryPath(configs.getStringProperty(Configs.KAFKA_CLUSTER_NAME)),
                configs.getStringProperty(Configs.CONSUMER_WORKLOAD_NODE_ID));
    }

    @Override
    public SupervisorController provide() {
        return ofNullable(availableImplementations.get(configs.getStringProperty(CONSUMER_WORKLOAD_ALGORITHM)))
                .orElseThrow(UnsupportedConsumerWorkloadAlgorithm::new).get();
    }

    @Override
    public void dispose(SupervisorController instance) {

    }
}
