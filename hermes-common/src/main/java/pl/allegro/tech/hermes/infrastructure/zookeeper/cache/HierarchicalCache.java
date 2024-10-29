package pl.allegro.tech.hermes.infrastructure.zookeeper.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class HierarchicalCache {

  private static final Logger logger = LoggerFactory.getLogger(HierarchicalCache.class);

  private final CuratorFramework curatorFramework;

  private final ExecutorService executorService;

  private final String basePath;
  private final boolean removeNodesWithNoData;

  private final List<String> levelPrefixes = new ArrayList<>();

  private final int maxDepth;

  private final List<CacheListeners> levelCallbacks = new ArrayList<>();

  private HierarchicalCacheLevel rootCache;

  public HierarchicalCache(
      CuratorFramework curatorFramework,
      ExecutorService executorService,
      String basePath,
      int maxDepth,
      List<String> levelPrefixes,
      boolean removeNodesWithNoData) {
    this.curatorFramework = curatorFramework;
    this.executorService = executorService;
    this.basePath = basePath;
    this.removeNodesWithNoData = removeNodesWithNoData;
    this.levelPrefixes.addAll(levelPrefixes);
    this.maxDepth = maxDepth;

    for (int i = 0; i < maxDepth; ++i) {
      levelCallbacks.add(new CacheListeners());
    }
  }

  public void start() throws Exception {
    ensureBasePath();
    rootCache = createLevelCache(0, basePath);
  }

  public void stop() throws Exception {
    rootCache.stop();
  }

  public void registerCallback(int depth, Consumer<PathChildrenCacheEvent> callback) {
    levelCallbacks.get(depth).addListener(callback);
  }

  private HierarchicalCacheLevel createLevelCache(int depth, String path) {
    BiFunction<Integer, String, HierarchicalCacheLevel> function =
        depth + 1 < maxDepth ? this::createLevelCache : null;
    HierarchicalCacheLevel levelCache =
        new HierarchicalCacheLevel(
            curatorFramework,
            executorService,
            path(depth, path),
            depth,
            levelCallbacks.get(depth),
            Optional.ofNullable(function),
            removeNodesWithNoData);
    try {
      logger.debug("Starting hierarchical cache level for path  {} and depth {}", path, depth);
      levelCache.start();
    } catch (Exception e) {
      logger.error(
          "Failed to start hierarchical cache level for path {} and depth {}",
          path(depth, path),
          depth,
          e);
    }
    return levelCache;
  }

  private String path(int depth, String basePath) {
    return basePath + (levelPrefixes.size() > depth ? "/" + levelPrefixes.get(depth) : "");
  }

  private void ensureBasePath() {
    try {
      try {
        if (curatorFramework.checkExists().forPath(basePath) == null) {
          curatorFramework.create().creatingParentsIfNeeded().forPath(basePath);
        }
      } catch (KeeperException.NodeExistsException e) {
        // ignore
      }
    } catch (Exception e) {
      throw new InternalProcessingException(e);
    }
  }
}
