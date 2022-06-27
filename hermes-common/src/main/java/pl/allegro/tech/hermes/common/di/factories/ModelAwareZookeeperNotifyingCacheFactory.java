package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.ModelAwareZookeeperNotifyingCache;

public class ModelAwareZookeeperNotifyingCacheFactory {

    private final CuratorFramework curator;

    private final ZookeeperParameters zookeeperParameters;

    public ModelAwareZookeeperNotifyingCacheFactory(CuratorFramework curator, ZookeeperParameters zookeeperParameters) {
        this.curator = curator;
        this.zookeeperParameters = zookeeperParameters;
    }

    public ModelAwareZookeeperNotifyingCache provide() {
        String rootPath = zookeeperParameters.getRoot();
        ModelAwareZookeeperNotifyingCache cache = new ModelAwareZookeeperNotifyingCache(
                curator, rootPath, zookeeperParameters.getProcessingThreadPoolSize()
        );
        try {
            cache.start();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to start Zookeeper cache for root path " + rootPath, e);
        }
        return cache;
    }
}
