package pl.allegro.tech.hermes.consumers.registry;

import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class ConsumerNodesRegistryPaths {

  private final ZookeeperPaths zookeeperPaths;
  private final String registryPath;

  public ConsumerNodesRegistryPaths(ZookeeperPaths zookeeperPaths, String clusterName) {
    this.zookeeperPaths = zookeeperPaths;
    this.registryPath =
        zookeeperPaths.join(zookeeperPaths.basePath(), "consumers-registry", clusterName);
  }

  public String leaderPath() {
    return zookeeperPaths.join(registryPath, "leader");
  }

  public String nodePath(String nodeId) {
    return zookeeperPaths.join(nodesPath(), nodeId);
  }

  public String nodesPath() {
    return zookeeperPaths.join(registryPath, "nodes");
  }
}
