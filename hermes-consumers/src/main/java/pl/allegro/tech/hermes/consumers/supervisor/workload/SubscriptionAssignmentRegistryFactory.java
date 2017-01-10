package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.IterableProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import javax.inject.Named;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_NODE_ID;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

public class SubscriptionAssignmentRegistryFactory implements Factory<SubscriptionAssignmentRegistry> {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionAssignmentRegistryFactory.class);

    private final CuratorFramework curatorClient;

    private final ConfigFactory configFactory;

    private final SubscriptionsCache cache;

    @Inject
    public SubscriptionAssignmentRegistryFactory(@Named(CuratorType.HERMES) CuratorFramework curatorClient,
                                                 ConfigFactory configFactory,
                                                 SubscriptionsCache cache) {
        this.curatorClient = curatorClient;
        this.configFactory = configFactory;
        this.cache = cache;
    }

    @Override
    public SubscriptionAssignmentRegistry provide() {
        return provide(configFactory.getStringProperty(CONSUMER_WORKLOAD_NODE_ID));
    }

    public SubscriptionAssignmentRegistry provide(String consumerId) {
        ZookeeperPaths paths = new ZookeeperPaths(configFactory.getStringProperty(Configs.ZOOKEEPER_ROOT));
        String cluster = configFactory.getStringProperty(KAFKA_CLUSTER_NAME);

        SubscriptionAssignmentRegistry registry = new SubscriptionAssignmentRegistry(
                consumerId,
                curatorClient,
                paths.consumersRuntimePath(cluster),
                cache,
                new SubscriptionAssignmentPathSerializer(paths.consumersRuntimePath(cluster))
        );

        return registry;
    }

    @Override
    public void dispose(SubscriptionAssignmentRegistry instance) {
        try {
            instance.stop();
        } catch (Exception e) {
            logger.warn("Unable to stop subscription assignment registry", e);
        }
    }
}
