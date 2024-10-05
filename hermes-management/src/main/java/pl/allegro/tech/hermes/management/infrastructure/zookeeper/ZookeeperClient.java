package pl.allegro.tech.hermes.management.infrastructure.zookeeper;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class ZookeeperClient {

  private static final Logger logger = getLogger(ZookeeperClient.class);
  private final CuratorFramework curatorFramework;
  private final String datacenterName;

  ZookeeperClient(CuratorFramework curatorFramework, String datacenterName) {
    this.curatorFramework = curatorFramework;
    this.datacenterName = datacenterName;
  }

  public CuratorFramework getCuratorFramework() {
    return curatorFramework;
  }

  public String getDatacenterName() {
    return datacenterName;
  }

  public void ensureEphemeralNodeExists(String path) {
    try {
      if (curatorFramework.checkExists().forPath(path) == null) {
        curatorFramework
            .create()
            .creatingParentsIfNeeded()
            .withMode(CreateMode.EPHEMERAL)
            .forPath(path);
      }
    } catch (Exception e) {
      throw new InternalProcessingException("Could not ensure existence of path: " + path);
    }
  }
}
