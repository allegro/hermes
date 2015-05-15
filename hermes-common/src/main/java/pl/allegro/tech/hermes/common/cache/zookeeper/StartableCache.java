package pl.allegro.tech.hermes.common.cache.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

public abstract class StartableCache<T> extends PathChildrenCache {

    public StartableCache(CuratorFramework curatorClient, String path, ExecutorService executorService) {
        super(curatorClient, path, true, false, executorService);
    }

    protected Collection<? extends T> callbacks;

    public void start(Collection<? extends T> callbacks) throws Exception {
        this.callbacks = callbacks;
        this.start();
    }
}
