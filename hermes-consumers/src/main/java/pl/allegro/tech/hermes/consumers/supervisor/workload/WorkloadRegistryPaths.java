package pl.allegro.tech.hermes.consumers.supervisor.workload;

import static pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths.CONSUMERS_WORKLOAD_PATH;

import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

class WorkloadRegistryPaths {

  private static final String WORKLOAD_RUNTIME_PATH = "runtime-bin";

  private final ZookeeperPaths zookeeperPaths;
  private final String clusterName;

  WorkloadRegistryPaths(ZookeeperPaths zookeeperPaths, String clusterName) {
    this.zookeeperPaths = zookeeperPaths;
    this.clusterName = clusterName;
  }

  String consumerWorkloadPath(String consumerId) {
    return zookeeperPaths.join(consumersWorkloadCurrentClusterRuntimeBinaryPath(), consumerId);
  }

  String consumersWorkloadCurrentClusterRuntimeBinaryPath() {
    return zookeeperPaths.join(
        zookeeperPaths.basePath(), CONSUMERS_WORKLOAD_PATH, clusterName, WORKLOAD_RUNTIME_PATH);
  }
}
