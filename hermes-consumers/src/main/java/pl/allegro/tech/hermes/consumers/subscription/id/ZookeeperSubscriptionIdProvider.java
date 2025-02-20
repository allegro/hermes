package pl.allegro.tech.hermes.consumers.subscription.id;

import java.util.Optional;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class ZookeeperSubscriptionIdProvider implements SubscriptionIdProvider {

  private final CuratorFramework curatorFramework;
  private final ZookeeperPaths zookeeperPaths;

  public ZookeeperSubscriptionIdProvider(
      CuratorFramework curatorFramework, ZookeeperPaths zookeeperPaths) {
    this.curatorFramework = curatorFramework;
    this.zookeeperPaths = zookeeperPaths;
  }

  @Override
  public SubscriptionId getSubscriptionId(SubscriptionName name) {
    return Optional.ofNullable(getZnodeStat(name))
        .map(Stat::getCzxid)
        .map(czxid -> SubscriptionId.from(name, czxid))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    String.format(
                        "Cannot get czxid of subscription %s as it doesn't exist",
                        name.getQualifiedName())));
  }

  private Stat getZnodeStat(SubscriptionName name) {
    String path = zookeeperPaths.subscriptionPath(name.getTopicName(), name.getName());
    try {
      return curatorFramework.checkExists().forPath(path);
    } catch (Exception e) {
      throw new InternalProcessingException(
          String.format(
              "Could not check existence of subscription %s node", name.getQualifiedName()),
          e);
    }
  }
}
