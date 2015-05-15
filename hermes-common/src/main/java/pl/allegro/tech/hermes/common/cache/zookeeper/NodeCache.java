package pl.allegro.tech.hermes.common.cache.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.di.CuratorType;

import javax.inject.Named;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class NodeCache<O, C extends StartableCache<O>> extends StartableCache<O> implements PathChildrenCacheListener {

    private static Logger logger = LoggerFactory.getLogger(NodeCache.class);

    protected final CuratorFramework curatorClient;
    protected final ObjectMapper objectMapper;
    protected final ExecutorService executorService;

    private final ReadWriteLock subcacheLock = new ReentrantReadWriteLock(true);

    private Map<String, C> subcacheMap = new HashedMap<>();

    public NodeCache(@Named(CuratorType.HERMES) CuratorFramework curatorClient, ObjectMapper objectMapper,
                     String path, ExecutorService executorService) {
        super(curatorClient, path, executorService);
        this.curatorClient = curatorClient;
        this.objectMapper = objectMapper;
        this.executorService = executorService;
        getListenable().addListener(this);
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        if (event.getData() == null) {
            return;
        }
        String path = event.getData().getPath();
        String cacheName = cacheNameFromPath(path);
        logger.info("Got entry change event for path {}", path);
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
    }

    protected C getEntry(String name) {
        Lock readLock = subcacheLock.readLock();
        readLock.lock();
        try {
            return subcacheMap.get(name);
        } finally {
            readLock.unlock();
        }
    }

    public void stop() throws IOException {
        this.callbacks = null;
        Lock writeLock = subcacheLock.writeLock();
        writeLock.lock();
        try {
            for (C subcache : subcacheMap.values()) {
                subcache.close();
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
                logger.info("Possible duplicate of new entry for {}, ignoring", cacheName);
                return;
            }
            C subcache = createSubcache(path);
            subcache.start(callbacks);
            subcacheMap.put(cacheName, subcache);
        } finally {
            writeLock.unlock();
        }

    }

    private void removeSubcache(String cacheName) throws Exception {
        Lock writeLock = subcacheLock.writeLock();
        writeLock.lock();
        try {
            C subcache = subcacheMap.remove(cacheName);
            if (subcache == null) {
                logger.info("Possible duplicate of removed entry for {}, ignoring", cacheName);
                return;
            }
            subcache.close();
        } finally {
            writeLock.unlock();
        }
    }

    protected abstract C createSubcache(String path);

    private String cacheNameFromPath(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
