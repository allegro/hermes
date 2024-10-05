package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.workload.constraints.ConsumersWorkloadConstraints;

class ZookeeperWorkloadConstraintsCache extends PathChildrenCache
    implements PathChildrenCacheListener {

  private static final Logger logger =
      LoggerFactory.getLogger(ZookeeperWorkloadConstraintsCache.class);

  private final Map<TopicName, Constraints> topicConstraintsCache = new ConcurrentHashMap<>();
  private final Map<SubscriptionName, Constraints> subscriptionConstraintsCache =
      new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper;
  private final ZookeeperPaths paths;

  ZookeeperWorkloadConstraintsCache(
      CuratorFramework curatorFramework, ObjectMapper objectMapper, ZookeeperPaths paths) {
    super(curatorFramework, paths.consumersWorkloadConstraintsPath(), true);
    this.objectMapper = objectMapper;
    this.paths = paths;
    getListenable().addListener(this);
  }

  ConsumersWorkloadConstraints getConsumersWorkloadConstraints() {
    return new ConsumersWorkloadConstraints(topicConstraintsCache, subscriptionConstraintsCache);
  }

  @Override
  public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) {
    switch (event.getType()) {
      case CHILD_ADDED:
        updateCache(event.getData().getPath(), event.getData().getData());
        break;
      case CHILD_REMOVED:
        removeFromCache(event.getData().getPath());
        break;
      case CHILD_UPDATED:
        updateCache(event.getData().getPath(), event.getData().getData());
        break;
      default:
        break;
    }
  }

  private void updateCache(String path, byte[] bytes) {
    Optional<Constraints> constraints = bytesToConstraints(bytes, path);
    if (!constraints.isPresent()) {
      return;
    }
    if (isSubscription(path)) {
      subscriptionConstraintsCache.put(
          SubscriptionName.fromString(
              paths.extractChildNode(path, paths.consumersWorkloadConstraintsPath())),
          constraints.get());
    } else {
      topicConstraintsCache.put(
          TopicName.fromQualifiedName(
              paths.extractChildNode(path, paths.consumersWorkloadConstraintsPath())),
          constraints.get());
    }
  }

  private Optional<Constraints> bytesToConstraints(byte[] bytes, String path) {
    try {
      return Optional.ofNullable(objectMapper.readValue(bytes, Constraints.class));
    } catch (Exception e) {
      logger.error("Cannot read data from node: {}", path, e);
      return Optional.empty();
    }
  }

  private void removeFromCache(String path) {
    if (isSubscription(path)) {
      subscriptionConstraintsCache.remove(
          SubscriptionName.fromString(
              paths.extractChildNode(path, paths.consumersWorkloadConstraintsPath())));
    } else {
      topicConstraintsCache.remove(
          TopicName.fromQualifiedName(
              paths.extractChildNode(path, paths.consumersWorkloadConstraintsPath())));
    }
  }

  private boolean isSubscription(String path) {
    return path.contains("$");
  }
}
