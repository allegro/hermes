package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import java.util.Optional;

public class KafkaCuratorClientFactory implements Factory<CuratorFramework> {

    private final ConfigFactory configFactory;
    private final CuratorClientFactory curatorClientFactory;

    @Inject
    public KafkaCuratorClientFactory(ConfigFactory configFactory, CuratorClientFactory curatorClientFactory) {
        this.configFactory = configFactory;
        this.curatorClientFactory = curatorClientFactory;
    }

    @Override
    public CuratorFramework provide() {
        String connectString = configFactory.getStringProperty(Configs.KAFKA_ZOOKEEPER_CONNECT_STRING);
        Optional<CuratorClientFactory.ZookeeperAuthorization> authorization = Optional.empty();

        if (configFactory.getBooleanProperty(Configs.KAFKA_ZOOKEEPER_AUTHORIZATION_ENABLED)) {
            authorization = Optional.of(new CuratorClientFactory.ZookeeperAuthorization(
                    configFactory.getStringProperty(Configs.KAFKA_ZOOKEEPER_AUTHORIZATION_SCHEME),
                    configFactory.getStringProperty(Configs.KAFKA_ZOOKEEPER_AUTHORIZATION_USER),
                    configFactory.getStringProperty(Configs.KAFKA_ZOOKEEPER_AUTHORIZATION_PASSWORD))
            );
        }

        return curatorClientFactory.provide(connectString, authorization);
    }

    @Override
    public void dispose(CuratorFramework instance) {
        instance.close();
    }
}
