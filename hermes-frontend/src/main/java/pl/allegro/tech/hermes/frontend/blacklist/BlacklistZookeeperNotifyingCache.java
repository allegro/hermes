package pl.allegro.tech.hermes.frontend.blacklist;

import java.util.ArrayList;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class BlacklistZookeeperNotifyingCache extends PathChildrenCache
    implements PathChildrenCacheListener {

  private static final Logger logger =
      LoggerFactory.getLogger(BlacklistZookeeperNotifyingCache.class);

  private final List<TopicBlacklistCallback> topicCallbacks = new ArrayList<>();

  public BlacklistZookeeperNotifyingCache(CuratorFramework curator, ZookeeperPaths paths) {
    super(curator, paths.topicsBlacklistPath(), true);
    getListenable().addListener(this);
  }

  @Override
  public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
    if (event == null || event.getData() == null) {
      return;
    }

    logger.info("Got {} event for path {}", event.getType(), event.getData().getPath());

    String qualifiedTopicName = getTopicName(event);

    switch (event.getType()) {
      case CHILD_ADDED:
        topicCallbacks.forEach(callback -> callback.onTopicBlacklisted(qualifiedTopicName));
        break;
      case CHILD_REMOVED:
        topicCallbacks.forEach(callback -> callback.onTopicUnblacklisted(qualifiedTopicName));
        break;
      default:
        break;
    }
  }

  public void startup() {
    try {
      this.start();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to start Zookeeper Topic Blacklist cache", e);
    }
  }

  private String getTopicName(PathChildrenCacheEvent event) {
    String[] paths = event.getData().getPath().split("/");
    return paths[paths.length - 1];
  }

  public void addCallback(TopicBlacklistCallback callback) {
    topicCallbacks.add(callback);
  }
}
