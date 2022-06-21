package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.config.Configs;

import java.util.Optional;

public class HermesCuratorClientFactory {

    private final CuratorZookeeperParameters zookeeperAuthorizationParameters;
    private final CuratorClientFactory curatorClientFactory;

    public HermesCuratorClientFactory(CuratorZookeeperParameters zookeeperAuthorizationParameters, CuratorClientFactory curatorClientFactory) {
        this.zookeeperAuthorizationParameters = zookeeperAuthorizationParameters;
        this.curatorClientFactory = curatorClientFactory;
    }

    public CuratorFramework provide() {
        Optional<CuratorClientFactory.ZookeeperAuthorization> authorization = Optional.empty();

        if (zookeeperAuthorizationParameters.isEnabled()) {
            authorization = Optional.of(new CuratorClientFactory.ZookeeperAuthorization(
                    zookeeperAuthorizationParameters.getScheme(),
                    zookeeperAuthorizationParameters.getUser(),
                    zookeeperAuthorizationParameters.getPassword())
            );
        }

        return curatorClientFactory.provide(zookeeperAuthorizationParameters.getConnectString(), authorization);
    }
}
