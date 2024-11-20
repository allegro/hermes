package pl.allegro.tech.hermes.infrastructure.zookeeper.cache;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class ModelAwareZookeeperNotifyingCache {

  private static final Logger logger =
      LoggerFactory.getLogger(ModelAwareZookeeperNotifyingCache.class);

  private static final int GROUP_LEVEL = 0;

  private static final int TOPIC_LEVEL = 1;

  private static final int SUBSCRIPTION_LEVEL = 2;

  private final HierarchicalCache cache;
  private final ExecutorService executor;

  public ModelAwareZookeeperNotifyingCache(
      CuratorFramework curator, ExecutorService executor, String rootPath) {
    List<String> levelPrefixes =
        Arrays.asList(
            ZookeeperPaths.GROUPS_PATH,
            ZookeeperPaths.TOPICS_PATH,
            ZookeeperPaths.SUBSCRIPTIONS_PATH);
    this.executor = executor;
    this.cache = new HierarchicalCache(curator, executor, rootPath, 3, levelPrefixes, true);
  }

  public void start() throws Exception {
    cache.start();
  }

  public void stop() {
    try {
      cache.stop();
      executor.shutdownNow();
    } catch (Exception e) {
      logger.warn("Failed to stop Zookeeper cache", e);
    }
  }

  public void registerGroupCallback(Consumer<PathChildrenCacheEvent> callback) {
    cache.registerCallback(GROUP_LEVEL, callback);
  }

  public void registerTopicCallback(Consumer<PathChildrenCacheEvent> callback) {
    cache.registerCallback(TOPIC_LEVEL, callback);
  }

  public void registerSubscriptionCallback(Consumer<PathChildrenCacheEvent> callback) {
    cache.registerCallback(SUBSCRIPTION_LEVEL, callback);
  }
}
