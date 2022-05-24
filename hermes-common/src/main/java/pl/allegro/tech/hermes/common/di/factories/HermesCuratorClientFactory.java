package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import java.util.Optional;

public class HermesCuratorClientFactory {

    private final ConfigFactory configFactory;
    private final CuratorClientFactory curatorClientFactory;

    public HermesCuratorClientFactory(ConfigFactory configFactory, CuratorClientFactory curatorClientFactory) {
        this.configFactory = configFactory;
        this.curatorClientFactory = curatorClientFactory;
    }

    public CuratorFramework provide() {
        String connectString = configFactory.getStringProperty(Configs.ZOOKEEPER_CONNECT_STRING);

        Optional<CuratorClientFactory.ZookeeperAuthorization> authorization = Optional.empty();

        if (configFactory.getBooleanProperty(Configs.ZOOKEEPER_AUTHORIZATION_ENABLED)) {
            authorization = Optional.of(new CuratorClientFactory.ZookeeperAuthorization(
                    configFactory.getStringProperty(Configs.ZOOKEEPER_AUTHORIZATION_SCHEME),
                    configFactory.getStringProperty(Configs.ZOOKEEPER_AUTHORIZATION_USER),
                    configFactory.getStringProperty(Configs.ZOOKEEPER_AUTHORIZATION_PASSWORD))
            );
        }

        return curatorClientFactory.provide(connectString, authorization);
    }
}
