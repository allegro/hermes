package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import static pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths.CONSUMERS_WORKLOAD_PATH;

class FlatBinaryWorkloadRegistryPaths {

    private final static String WORKLOAD_BINARY_RUNTIME_PATH = "runtime-bin";

    private final ZookeeperPaths zookeeperPaths;
    private final String clusterName;

    FlatBinaryWorkloadRegistryPaths(ZookeeperPaths zookeeperPaths, String clusterName) {
        this.zookeeperPaths = zookeeperPaths;
        this.clusterName = clusterName;
    }

    String consumerWorkloadPath(String consumerId) {
        return zookeeperPaths.join(consumersWorkloadCurrentClusterRuntimeBinaryPath(), consumerId);
    }

    String consumersWorkloadCurrentClusterRuntimeBinaryPath() {
        return zookeeperPaths.join(zookeeperPaths.basePath(), CONSUMERS_WORKLOAD_PATH, clusterName, WORKLOAD_BINARY_RUNTIME_PATH);
    }
}
