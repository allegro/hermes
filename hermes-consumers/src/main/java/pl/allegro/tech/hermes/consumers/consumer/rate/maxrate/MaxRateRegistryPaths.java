package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import static pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths.CONSUMERS_RATE_PATH;
import static pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths.MAX_RATE_HISTORY_PATH;
import static pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths.MAX_RATE_PATH;

import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

class MaxRateRegistryPaths {

  private static final String RATE_RUNTIME_PATH = "runtime-bin";

  private final ZookeeperPaths zookeeperPaths;
  private final String clusterName;
  private final String currentConsumerRateHistoryPath;

  MaxRateRegistryPaths(
      ZookeeperPaths zookeeperPaths, String currentConsumerId, String clusterName) {
    this.zookeeperPaths = zookeeperPaths;
    this.clusterName = clusterName;
    this.currentConsumerRateHistoryPath = consumerRateHistoryPath(currentConsumerId);
  }

  String consumerMaxRatePath(String consumerId) {
    return zookeeperPaths.join(consumerRateParentRuntimePath(consumerId), MAX_RATE_PATH);
  }

  String consumerRateHistoryPath(String consumerId) {
    return zookeeperPaths.join(consumerRateParentRuntimePath(consumerId), MAX_RATE_HISTORY_PATH);
  }

  String consumerRateParentRuntimePath(String consumerId) {
    return zookeeperPaths.join(consumersRateCurrentClusterRuntimeBinaryPath(), consumerId);
  }

  String consumersRateCurrentClusterRuntimeBinaryPath() {
    return zookeeperPaths.join(
        zookeeperPaths.basePath(), CONSUMERS_RATE_PATH, clusterName, RATE_RUNTIME_PATH);
  }

  String currentConsumerRateHistoryPath() {
    return currentConsumerRateHistoryPath;
  }
}
