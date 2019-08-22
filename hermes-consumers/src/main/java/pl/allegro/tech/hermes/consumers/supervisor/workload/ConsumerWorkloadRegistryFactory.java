package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerWorkloadAlgorithm.SELECTIVE;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentRegistry.AUTO_ASSIGNED_MARKER;

public class ConsumerWorkloadRegistryFactory implements Factory<ConsumerWorkloadRegistry> {

    private static final Logger logger = getLogger(ConsumerWorkloadRegistryFactory.class);

    private final CuratorFramework curator;
    private final ConfigFactory configFactory;
    private final ZookeeperPaths zookeeperPaths;
    private final SubscriptionAssignmentNotifyingCache subscriptionAssignmentNotifyingCache;

    @Inject
    public ConsumerWorkloadRegistryFactory(
            CuratorFramework curator,
            ConfigFactory configFactory,
            ZookeeperPaths zookeeperPaths,
            SubscriptionAssignmentNotifyingCache subscriptionAssignmentNotifyingCache) {
        this.curator = curator;
        this.configFactory = configFactory;
        this.zookeeperPaths = zookeeperPaths;
        this.subscriptionAssignmentNotifyingCache = subscriptionAssignmentNotifyingCache;
    }

    @Override
    public ConsumerWorkloadRegistry provide() {
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
                return new SubscriptionAssignmentRegistry(
                        curator,
                        subscriptionAssignmentNotifyingCache,
                        pathSerializer,
                        assignmentNodeCreationMode
                );
            case FLAT_BINARY:
                throw new UnsupportedOperationException("Not implemented yet.");
            default:
                throw new UnsupportedOperationException("Max-rate type not supported.");
        }
    }

    private CreateMode getAssignmentNodeCreationMode() {
        String algorithm = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_ALGORITHM);
        return SELECTIVE.equals(algorithm) ? CreateMode.PERSISTENT : CreateMode.EPHEMERAL;
    }

    @Override
    public void dispose(ConsumerWorkloadRegistry instance) {

    }
}
