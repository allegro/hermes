package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;

public class ClusterAssignmentCacheFactory implements Factory<ClusterAssignmentCache> {

    private static final Logger logger = getLogger(ClusterAssignmentCacheFactory.class);

    private final CuratorFramework curator;
    private final ConfigFactory configFactory;
    private final ConsumerAssignmentCache consumerAssignmentCache;
    private final ZookeeperPaths zookeeperPaths;
    private final SubscriptionIds subscriptionIds;
    private final ConsumerNodesRegistry consumerNodesRegistry;

    @Inject
    public ClusterAssignmentCacheFactory(CuratorFramework curator,
                                         ConfigFactory configFactory,
                                         ConsumerAssignmentCache consumerAssignmentCache,
                                         ZookeeperPaths zookeeperPaths,
                                         SubscriptionIds subscriptionIds,
                                         ConsumerNodesRegistry consumerNodesRegistry) {
        this.curator = curator;
        this.configFactory = configFactory;
        this.consumerAssignmentCache = consumerAssignmentCache;
        this.zookeeperPaths = zookeeperPaths;
        this.subscriptionIds = subscriptionIds;
        this.consumerNodesRegistry = consumerNodesRegistry;
    }

    @Override
    public ClusterAssignmentCache provide() {
        ConsumerWorkloadRegistryType type;
        try {
            String typeString = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_REGISTRY_TYPE);
            type = ConsumerWorkloadRegistryType.fromString(typeString);
        } catch (Exception e) {
            logger.error("Could not configure subscription assignment notifying repository based on specified consumer workload registry type", e);
            throw e;
        }

        String clusterName = configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME);

        switch (type) {
            case HIERARCHICAL:
                if (consumerAssignmentCache instanceof HierarchicalConsumerAssignmentCache) {
                    return (HierarchicalConsumerAssignmentCache) consumerAssignmentCache;
                } else {
                    throw new IllegalStateException("Invalid type of HierarchicalConsumerAssignmentCache was registered for this type of workload registry");
                }
            case FLAT_BINARY:
                return new FlatBinaryClusterAssignmentCache(curator, clusterName, zookeeperPaths, subscriptionIds, consumerNodesRegistry);
            default:
                throw new UnsupportedOperationException("Workload registry type not supported.");
        }
    }

    @Override
    public void dispose(ClusterAssignmentCache instance) {

    }
}
