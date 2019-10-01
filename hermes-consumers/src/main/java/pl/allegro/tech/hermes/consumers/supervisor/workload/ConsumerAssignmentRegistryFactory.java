package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerWorkloadAlgorithm.SELECTIVE;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.HierarchicalConsumerAssignmentRegistry.AUTO_ASSIGNED_MARKER;

public class ConsumerAssignmentRegistryFactory implements Factory<ConsumerAssignmentRegistry> {

    private static final Logger logger = getLogger(ConsumerAssignmentRegistryFactory.class);

    private final CuratorFramework curator;
    private final ConfigFactory configFactory;
    private final ZookeeperPaths zookeeperPaths;
    private final ConsumerAssignmentCache consumerAssignmentCache;
    private final SubscriptionIds subscriptionIds;

    @Inject
    public ConsumerAssignmentRegistryFactory(
            CuratorFramework curator,
            ConfigFactory configFactory,
            ZookeeperPaths zookeeperPaths,
            ConsumerAssignmentCache consumerAssignmentCache,
            SubscriptionIds subscriptionIds) {
        this.curator = curator;
        this.configFactory = configFactory;
        this.zookeeperPaths = zookeeperPaths;
        this.consumerAssignmentCache = consumerAssignmentCache;
        this.subscriptionIds = subscriptionIds;
    }

    @Override
    public ConsumerAssignmentRegistry provide() {
        ConsumerWorkloadRegistryType type;
        try {
            String typeString = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_REGISTRY_TYPE);
            type = ConsumerWorkloadRegistryType.fromString(typeString);
        } catch (Exception e) {
            logger.error("Could not configure consumer workload registry", e);
            throw e;
        }
        logger.info("Consumer workload registry type chosen: {}", type.getConfigValue());

        switch (type) {
            case HIERARCHICAL:
                String cluster = configFactory.getStringProperty(KAFKA_CLUSTER_NAME);
                String consumersRuntimePath = zookeeperPaths.consumersRuntimePath(cluster);
                SubscriptionAssignmentPathSerializer pathSerializer = new SubscriptionAssignmentPathSerializer(consumersRuntimePath, AUTO_ASSIGNED_MARKER);
                CreateMode assignmentNodeCreationMode = getAssignmentNodeCreationMode();
                return new HierarchicalConsumerAssignmentRegistry(
                        curator,
                        consumerAssignmentCache,
                        pathSerializer,
                        assignmentNodeCreationMode
                );
            case FLAT_BINARY:
                return new FlatBinaryConsumerAssignmentRegistry(curator, configFactory, zookeeperPaths, subscriptionIds);
            default:
                throw new UnsupportedOperationException("Max-rate type not supported.");
        }
    }

    private CreateMode getAssignmentNodeCreationMode() {
        String algorithm = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_ALGORITHM);
        return SELECTIVE.equals(algorithm) ? CreateMode.PERSISTENT : CreateMode.EPHEMERAL;
    }

    @Override
    public void dispose(ConsumerAssignmentRegistry instance) {

    }
}
