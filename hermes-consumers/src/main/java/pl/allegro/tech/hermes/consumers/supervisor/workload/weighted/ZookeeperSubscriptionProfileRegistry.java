package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths.CONSUMERS_WORKLOAD_PATH;
import static pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths.SUBSCRIPTION_PROFILES_PATH;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class ZookeeperSubscriptionProfileRegistry implements SubscriptionProfileRegistry {

  private static final Logger logger =
      LoggerFactory.getLogger(ZookeeperSubscriptionProfileRegistry.class);

  private final CuratorFramework curator;
  private final SubscriptionProfilesEncoder encoder;
  private final SubscriptionProfilesDecoder decoder;
  private final String profilesPath;

  public ZookeeperSubscriptionProfileRegistry(
      CuratorFramework curator,
      SubscriptionIds subscriptionIds,
      ZookeeperPaths zookeeperPaths,
      String clusterName,
      int subscriptionProfilesEncoderBufferSizeBytes) {
    this.curator = curator;
    this.encoder =
        new SubscriptionProfilesEncoder(
            subscriptionIds, subscriptionProfilesEncoderBufferSizeBytes);
    this.decoder = new SubscriptionProfilesDecoder(subscriptionIds);
    this.profilesPath =
        zookeeperPaths.join(
            zookeeperPaths.basePath(),
            CONSUMERS_WORKLOAD_PATH,
            clusterName,
            SUBSCRIPTION_PROFILES_PATH);
  }

  @Override
  public SubscriptionProfiles fetch() {
    try {
      if (curator.checkExists().forPath(profilesPath) != null) {
        byte[] bytes = curator.getData().forPath(profilesPath);
        return decoder.decode(bytes);
      }
    } catch (Exception e) {
      logger.warn("Could not read node data on path " + profilesPath, e);
    }
    return SubscriptionProfiles.EMPTY;
  }

  @Override
  public void persist(SubscriptionProfiles profiles) {
    byte[] encoded = encoder.encode(profiles);
    try {
      updateOrCreate(encoded);
    } catch (Exception e) {
      logger.error("An error while saving subscription profiles", e);
    }
  }

  private void updateOrCreate(byte[] encoded) throws Exception {
    try {
      curator.setData().forPath(profilesPath, encoded);
    } catch (KeeperException.NoNodeException e) {
      try {
        curator
            .create()
            .creatingParentContainersIfNeeded()
            .withMode(CreateMode.PERSISTENT)
            .forPath(profilesPath, encoded);
      } catch (KeeperException.NodeExistsException ex) {
        // ignore
      }
    }
  }
}
