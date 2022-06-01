package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.collect.Sets;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public class ConsumerAssignmentRegistry {
    private static final Logger logger = getLogger(ConsumerAssignmentRegistry.class);

    private final ZookeeperOperations zookeeper;
    private final ConsumerWorkloadEncoder consumerAssignmentsEncoder;
    private final WorkloadRegistryPaths paths;

    public ConsumerAssignmentRegistry(CuratorFramework curator,
                                      ConfigFactory configFactory,
                                      ZookeeperPaths zookeeperPaths,
                                      SubscriptionIds subscriptionIds) {
        this.zookeeper = new ZookeeperOperations(curator);
        int assignmentsEncoderBufferSize = configFactory.getIntProperty(Configs.CONSUMER_WORKLOAD_REGISTRY_BINARY_ENCODER_ASSIGNMENTS_BUFFER_SIZE_BYTES);
        this.consumerAssignmentsEncoder = new ConsumerWorkloadEncoder(subscriptionIds, assignmentsEncoderBufferSize);

        final String clusterName = configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME);
        this.paths = new WorkloadRegistryPaths(zookeeperPaths, clusterName);
    }

    public WorkDistributionChanges updateAssignments(SubscriptionAssignmentView initialState,
                                                     SubscriptionAssignmentView targetState) {
        if (initialState.equals(targetState)) {
            return WorkDistributionChanges.NO_CHANGES;
        }
        SubscriptionAssignmentView deletions = initialState.deletions(targetState);
        SubscriptionAssignmentView additions = initialState.additions(targetState);
        Sets.SetView<String> modifiedConsumerNodes = Sets.union(
                deletions.getConsumerNodes(),
                additions.getConsumerNodes()
        );
        for (String consumerNode : modifiedConsumerNodes) {
            Set<SubscriptionName> subscriptions = targetState.getSubscriptionsForConsumerNode(consumerNode);
            byte[] encoded = consumerAssignmentsEncoder.encode(subscriptions);
            try {
                logger.info("Writing {} bytes of {} assignments for consumer {}",
                        encoded.length, subscriptions.size(), consumerNode);
                String path = paths.consumerWorkloadPath(consumerNode);
                zookeeper.writeOrCreatePersistent(path, encoded);
            } catch (Exception e) {
                logger.error("Could not write consumer workload for {}", consumerNode);
            }
        }
        return new WorkDistributionChanges(deletions.getAllAssignments().size(), additions.getAllAssignments().size());
    }
}
