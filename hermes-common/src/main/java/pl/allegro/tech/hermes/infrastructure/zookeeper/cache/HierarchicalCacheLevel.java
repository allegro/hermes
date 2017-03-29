package pl.allegro.tech.hermes.infrastructure.zookeeper.cache;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

class HierarchicalCacheLevel extends PathChildrenCache implements PathChildrenCacheListener {

    private static Logger logger = LoggerFactory.getLogger(HierarchicalCacheLevel.class);

    private final ReadWriteLock subcacheLock = new ReentrantReadWriteLock(true);

    private final CacheListeners consumer;

    private final int currentDepth;

    private final Optional<BiFunction<Integer, String, HierarchicalCacheLevel>> nextLevelFactory;

    private final Map<String, HierarchicalCacheLevel> subcacheMap = new HashMap<>();

    HierarchicalCacheLevel(CuratorFramework curatorClient,
                           ExecutorService executorService,
                           String path,
                           int depth,
                           CacheListeners eventConsumer,
                           Optional<BiFunction<Integer, String, HierarchicalCacheLevel>> nextLevelFactory) {
        super(curatorClient, path, true, false, executorService);
        this.currentDepth = depth;
        this.consumer = eventConsumer;
        this.nextLevelFactory = nextLevelFactory;
        getListenable().addListener(this);
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        if (event.getData() == null) {
            return;
        }

        String path = event.getData().getPath();
        String cacheName = cacheNameFromPath(path);
        logger.debug("Got {} event for path {}", event.getType(), path);

        switch (event.getType()) {
            case CHILD_ADDED:
                addSubcache(path, cacheName);
                break;
            case CHILD_REMOVED:
                removeSubcache(cacheName);
                break;
            default:
                break;
        }

        consumer.call(event);
    }

    void stop() throws IOException {
        Lock writeLock = subcacheLock.writeLock();
        writeLock.lock();
        try {
            for (HierarchicalCacheLevel subcache : subcacheMap.values()) {
                subcache.stop();
            }
            subcacheMap.clear();
            this.close();
        } finally {
            writeLock.unlock();
        }
    }

    private void addSubcache(String path, String cacheName) throws Exception {
        Lock writeLock = subcacheLock.writeLock();
        writeLock.lock();
        try {
            if (subcacheMap.containsKey(cacheName)) {
                logger.debug("Possible duplicate of new entry for {}, ignoring", cacheName);
                return;
            }
            nextLevelFactory.ifPresent(f -> subcacheMap.put(cacheName, f.apply(currentDepth + 1, path)));
        } finally {
            writeLock.unlock();
        }

    }

    private void removeSubcache(String cacheName) throws Exception {
        Lock writeLock = subcacheLock.writeLock();
        writeLock.lock();
        try {
            HierarchicalCacheLevel subcache = subcacheMap.remove(cacheName);
            if (subcache == null) {
                logger.debug("Possible duplicate of removed entry for {}, ignoring", cacheName);
                return;
            }
            subcache.close();
        } finally {
            writeLock.unlock();
        }
    }

    private String cacheNameFromPath(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
