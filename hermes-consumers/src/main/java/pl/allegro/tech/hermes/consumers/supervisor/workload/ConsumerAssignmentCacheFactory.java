package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;

public class ConsumerAssignmentCacheFactory implements Factory<ConsumerAssignmentCache> {

    private static final Logger logger = getLogger(ConsumerAssignmentCacheFactory.class);

    private final CuratorFramework curator;
    private final ConfigFactory configFactory;
    private final ZookeeperPaths zookeeperPaths;
    private final SubscriptionsCache subscriptionsCache;
    private final SubscriptionIds subscriptionIds;

    @Inject
    public ConsumerAssignmentCacheFactory(
            CuratorFramework curator,
            ConfigFactory configFactory,
            ZookeeperPaths zookeeperPaths,
            SubscriptionsCache subscriptionsCache,
            SubscriptionIds subscriptionIds
    ) {
        this.curator = curator;
        this.configFactory = configFactory;
        this.zookeeperPaths = zookeeperPaths;
        this.subscriptionsCache = subscriptionsCache;
        this.subscriptionIds = subscriptionIds;
    }

    @Override
    public ConsumerAssignmentCache provide() {
        ConsumerWorkloadRegistryType type;
        try {
            String typeString = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_REGISTRY_TYPE);
            type = ConsumerWorkloadRegistryType.fromString(typeString);
        } catch (Exception e) {
            logger.error("Could not configure consumer assignment notifying cache based on specified consumer workload registry type", e);
            throw e;
        }

        String consumerId = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_NODE_ID);
        String clusterName = configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME);

        switch (type) {
            case HIERARCHICAL:
                return new HierarchicalConsumerAssignmentCache(curator, consumerId, clusterName, zookeeperPaths, subscriptionsCache);
            case FLAT_BINARY:
                return new FlatBinaryConsumerAssignmentCache(curator, consumerId, clusterName, zookeeperPaths, subscriptionIds);
            default:
                throw new UnsupportedOperationException("Workload registry type not supported.");
        }
    }

    @Override
    public void dispose(ConsumerAssignmentCache instance) {

    }
}
