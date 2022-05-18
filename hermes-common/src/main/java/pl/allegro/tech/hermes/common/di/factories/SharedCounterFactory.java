package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;

public class SharedCounterFactory {

    private final CuratorFramework zookeeper;

    private final ConfigFactory config;

    public SharedCounterFactory(CuratorFramework zookeeper, ConfigFactory config) {
        this.zookeeper = zookeeper;
        this.config = config;
    }

    public SharedCounter provide() {
        return new SharedCounter(zookeeper,
                config.getIntProperty(Configs.METRICS_COUNTER_EXPIRE_AFTER_ACCESS),
                config.getIntProperty(Configs.ZOOKEEPER_BASE_SLEEP_TIME),
                config.getIntProperty(Configs.ZOOKEEPER_MAX_RETRIES)
        );
    }
}
