package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.ModelAwareZookeeperNotifyingCache;

import javax.inject.Inject;
import javax.inject.Named;

import static pl.allegro.tech.hermes.common.config.Configs.ZOOKEEPER_TASK_PROCESSING_THREAD_POOL_SIZE;

public class ModelAwareZookeeperNotifyingCacheFactory implements Factory<ModelAwareZookeeperNotifyingCache> {

    private static final Logger logger = LoggerFactory.getLogger(ModelAwareZookeeperNotifyingCacheFactory.class);

    private final CuratorFramework curator;

    private final ConfigFactory config;

    @Inject
    public ModelAwareZookeeperNotifyingCacheFactory(@Named(CuratorType.HERMES) CuratorFramework curator, ConfigFactory config) {
        this.curator = curator;
        this.config = config;
    }

    @Override
    public ModelAwareZookeeperNotifyingCache provide() {
        String rootPath = config.getStringProperty(Configs.ZOOKEEPER_ROOT);
        ModelAwareZookeeperNotifyingCache cache = new ModelAwareZookeeperNotifyingCache(
                curator, rootPath, config.getIntProperty(ZOOKEEPER_TASK_PROCESSING_THREAD_POOL_SIZE)
        );
        try {
            cache.start();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to start Zookeeper cache for root path " + rootPath, e);
        }
        return cache;
    }

    @Override
    public void dispose(ModelAwareZookeeperNotifyingCache instance) {
        instance.stop();
    }
}
