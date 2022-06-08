package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.ModelAwareZookeeperNotifyingCache;

import static pl.allegro.tech.hermes.common.config.Configs.ZOOKEEPER_TASK_PROCESSING_THREAD_POOL_SIZE;

public class ModelAwareZookeeperNotifyingCacheFactory {

    private final CuratorFramework curator;

    private final ConfigFactory config;

    public ModelAwareZookeeperNotifyingCacheFactory(CuratorFramework curator, ConfigFactory config) {
        this.curator = curator;
        this.config = config;
    }

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
}
