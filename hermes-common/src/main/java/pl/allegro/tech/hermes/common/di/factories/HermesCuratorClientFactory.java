package pl.allegro.tech.hermes.common.di.factories;

import java.util.Optional;
import org.apache.curator.framework.CuratorFramework;

public class HermesCuratorClientFactory {

  private final ZookeeperParameters zookeeperParameters;
  private final CuratorClientFactory curatorClientFactory;

  public HermesCuratorClientFactory(
      ZookeeperParameters zookeeperParameters, CuratorClientFactory curatorClientFactory) {
    this.zookeeperParameters = zookeeperParameters;
    this.curatorClientFactory = curatorClientFactory;
  }

  public CuratorFramework provide() {
    String connectString = zookeeperParameters.getConnectionString();

    Optional<CuratorClientFactory.ZookeeperAuthorization> authorization = Optional.empty();

    if (zookeeperParameters.isAuthorizationEnabled()) {
      authorization =
          Optional.of(
              new CuratorClientFactory.ZookeeperAuthorization(
                  zookeeperParameters.getScheme(),
                  zookeeperParameters.getUser(),
                  zookeeperParameters.getPassword()));
    }

    return curatorClientFactory.provide(connectString, authorization);
  }
}
