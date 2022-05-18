package pl.allegro.tech.hermes.common.di.factories;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class ZookeeperPathsFactory {

    private final ConfigFactory config;

    public ZookeeperPathsFactory(ConfigFactory config) {
        this.config = config;
    }

    public ZookeeperPaths provide() {
        return new ZookeeperPaths(config.getStringProperty(Configs.ZOOKEEPER_ROOT));
    }
}
