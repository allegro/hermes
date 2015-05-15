package pl.allegro.tech.hermes.common.di.factories;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;

public class ZookeeperPathsFactory implements Factory<ZookeeperPaths> {

    private final ConfigFactory config;

    @Inject
    public ZookeeperPathsFactory(ConfigFactory config) {
        this.config = config;
    }

    @Override
    public ZookeeperPaths provide() {
        return new ZookeeperPaths(config.getStringProperty(Configs.ZOOKEEPER_ROOT));
    }

    @Override
    public void dispose(ZookeeperPaths instance) {
    }
}
