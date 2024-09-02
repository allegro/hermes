package pl.allegro.tech.hermes.consumers.supervisor.workload;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class ConsumerAssignmentRegistry {
  private static final Logger logger = getLogger(ConsumerAssignmentRegistry.class);

  private final ZookeeperOperations zookeeper;
  private final ConsumerWorkloadEncoder consumerAssignmentsEncoder;
  private final WorkloadRegistryPaths paths;

  public ConsumerAssignmentRegistry(
      CuratorFramework curator,
      int assignmentsEncoderBufferSize,
      String clusterName,
      ZookeeperPaths zookeeperPaths,
      SubscriptionIds subscriptionIds) {
    this.zookeeper = new ZookeeperOperations(curator);
    this.consumerAssignmentsEncoder =
        new ConsumerWorkloadEncoder(subscriptionIds, assignmentsEncoderBufferSize);

    this.paths = new WorkloadRegistryPaths(zookeeperPaths, clusterName);
  }

  public void updateAssignments(String consumerNode, Set<SubscriptionName> subscriptions) {
    byte[] encoded = consumerAssignmentsEncoder.encode(subscriptions);
    try {
      String path = paths.consumerWorkloadPath(consumerNode);
      zookeeper.writeOrCreatePersistent(path, encoded);
    } catch (Exception e) {
      logger.error("Could not write consumer workload for {}", consumerNode);
    }
  }
}
