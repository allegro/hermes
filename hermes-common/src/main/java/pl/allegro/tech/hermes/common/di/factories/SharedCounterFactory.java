package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;

import javax.inject.Inject;
import javax.inject.Named;

public class SharedCounterFactory implements Factory<SharedCounter> {

    private final CuratorFramework zookeeper;

    private final ConfigFactory config;

    @Inject
    public SharedCounterFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper, ConfigFactory config) {
        this.zookeeper = zookeeper;
        this.config = config;
    }

    @Override
    public SharedCounter provide() {
        return new SharedCounter(zookeeper,
                config.getIntProperty(Configs.METRICS_COUNTER_EXPIRE_AFTER_ACCESS),
                config.getIntProperty(Configs.ZOOKEEPER_BASE_SLEEP_TIME),
                config.getIntProperty(Configs.ZOOKEEPER_MAX_RETRIES)
        );
    }

    @Override
    public void dispose(SharedCounter instance) {
    }
}
