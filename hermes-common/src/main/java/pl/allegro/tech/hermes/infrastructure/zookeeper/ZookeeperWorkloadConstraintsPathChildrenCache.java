package pl.allegro.tech.hermes.infrastructure.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ZookeeperWorkloadConstraintsPathChildrenCache extends PathChildrenCache implements PathChildrenCacheListener {

    private final Map<String, ChildData> cache = new ConcurrentHashMap<>();

    ZookeeperWorkloadConstraintsPathChildrenCache(CuratorFramework curatorFramework, String basePath) {
        super(curatorFramework, basePath, true);
        getListenable().addListener(this);
    }

    Collection<ChildData> getChildrenData() {
        return cache.values();
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) {
        switch (event.getType()) {
            case CHILD_ADDED:
                cache.put(event.getData().getPath(), event.getData());
                break;
            case CHILD_REMOVED:
                cache.remove(event.getData().getPath());
                break;
            case CHILD_UPDATED:
                cache.put(event.getData().getPath(), event.getData());
                break;
            default:
                break;
        }
    }
}
