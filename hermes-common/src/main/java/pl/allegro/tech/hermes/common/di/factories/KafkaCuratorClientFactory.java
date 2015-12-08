package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;

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
        return curatorClientFactory.provide(connectString);
    }

    @Override
    public void dispose(CuratorFramework instance) {
        instance.close();
    }
}
