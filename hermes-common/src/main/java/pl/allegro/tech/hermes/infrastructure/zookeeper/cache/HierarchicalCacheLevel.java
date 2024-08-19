package pl.allegro.tech.hermes.infrastructure.zookeeper.cache;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

class HierarchicalCacheLevel extends PathChildrenCache implements PathChildrenCacheListener {

    private static Logger logger = LoggerFactory.getLogger(HierarchicalCacheLevel.class);

    private final ReadWriteLock subcacheLock = new ReentrantReadWriteLock(true);

    private final CacheListeners consumer;

    private final CuratorFramework curatorClient;
    private final int currentDepth;

    private final Optional<BiFunction<Integer, String, HierarchicalCacheLevel>> nextLevelFactory;
    private final boolean removeNodesWithNoData;

    private final Map<String, HierarchicalCacheLevel> subcacheMap = new HashMap<>();

    HierarchicalCacheLevel(CuratorFramework curatorClient,
                           ExecutorService executorService,
                           String path,
                           int depth,
                           CacheListeners eventConsumer,
                           Optional<BiFunction<Integer, String, HierarchicalCacheLevel>> nextLevelFactory,
                           boolean removeNodesWithNoData) {
        super(curatorClient, path, true, false, executorService);
        this.curatorClient = curatorClient;
        this.currentDepth = depth;
        this.consumer = eventConsumer;
        this.nextLevelFactory = nextLevelFactory;
        this.removeNodesWithNoData = removeNodesWithNoData;
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
                addSubcache(path, cacheName, event.getData().getData());
                break;
            case CHILD_REMOVED:
                removeSubcache(path, cacheName);
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

    private void addSubcache(String path, String cacheName, byte[] data) {
        Lock writeLock = subcacheLock.writeLock();
        writeLock.lock();
        try {
            logger.debug("Adding cache for path {}; Cache name: {}; Depth: {}; InstanceId: {}",
                    path, cacheName, currentDepth, Integer.toHexString(hashCode()));

            if (ArrayUtils.isEmpty(data) && removeNodesWithNoData) {
                logger.warn("Removing path {} due to no data in the znode", path);
                printOrphanedChildren(path);
                removeNodeRecursively(path);
                return;
            }
            if (subcacheMap.containsKey(cacheName)) {
                logger.debug("Possible duplicate of new entry for {}, ignoring", cacheName);
                return;
            }
            nextLevelFactory.ifPresent(f -> subcacheMap.put(cacheName, f.apply(currentDepth + 1, path)));
        } finally {
            writeLock.unlock();
        }
    }

    private void printOrphanedChildren(String path) {
        try {
            List<String> children = curatorClient.getChildren().forPath(path);
            logger.warn("Nodes with empty parent {}: {}",
                    path, children.stream().map(Object::toString).collect(Collectors.joining(",")));
            printChildrenWithEmptyParentRecursively(path, children);
        } catch (KeeperException.NoNodeException e) {
            logger.info("Could not receive list of children for path {} as the path does not exist", path);
        } catch (Exception e) {
            logger.warn("Could not receive list of children for path {} due to error", path, e);
        }
    }

    private void printChildrenWithEmptyParentRecursively(String pathWithEmptyParent, List<String> children) {
        children.forEach(c -> {
            try {
                String nextPath = pathWithEmptyParent + "/" + c;
                logger.warn("Node with empty parent: {}", nextPath);
                List<String> nextChildren = curatorClient.getChildren().forPath(nextPath);

                printChildrenWithEmptyParentRecursively(nextPath, nextChildren);
            } catch (KeeperException.NoNodeException e) {
                logger.info("Could not receive list of children for path {} as the path does not exist", pathWithEmptyParent);
            } catch (Exception e) {
                logger.warn("Could not receive list of children for path {} due to error", pathWithEmptyParent, e);
            }
        });
    }

    private void removeNodeRecursively(String path) {
        try {
            curatorClient.delete().deletingChildrenIfNeeded().forPath(path);
            logger.warn("Removed recursively path {}", path);
        } catch (Exception e) {
            logger.warn("Error while deleting recursively path {}", path, e);
        }
    }

    private void removeSubcache(String path, String cacheName) throws Exception {
        Lock writeLock = subcacheLock.writeLock();
        writeLock.lock();
        logger.debug("Removing cache for path {}; Cache name: {}; Depth {}; InstanceId: {}",
                path, cacheName, currentDepth, Integer.toHexString(hashCode()));
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
