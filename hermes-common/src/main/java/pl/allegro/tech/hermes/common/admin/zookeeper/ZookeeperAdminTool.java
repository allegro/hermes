package pl.allegro.tech.hermes.common.admin.zookeeper;

import static pl.allegro.tech.hermes.common.admin.AdminTool.Operations.RETRANSMIT;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.common.exception.RetransmissionException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class ZookeeperAdminTool implements AdminTool {

  private final ZookeeperPaths zookeeperPaths;
  private final CuratorFramework curatorFramework;
  private final ObjectMapper objectMapper;

  public ZookeeperAdminTool(
      ZookeeperPaths zookeeperPaths, CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    this.zookeeperPaths = zookeeperPaths;
    this.curatorFramework = curatorFramework;
    this.objectMapper = objectMapper;
  }

  @Override
  public void retransmit(SubscriptionName subscriptionName) {
    try {
      executeAdminOperation(subscriptionName, RETRANSMIT.name());
    } catch (Exception e) {
      throw new RetransmissionException(e);
    }
  }

  private void executeAdminOperation(SubscriptionName subscriptionName, String name)
      throws Exception {
    String path = zookeeperPaths.adminOperationPath(name);

    curatorFramework
        .create()
        .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
        .forPath(path, objectMapper.writeValueAsBytes(subscriptionName));
  }
}
