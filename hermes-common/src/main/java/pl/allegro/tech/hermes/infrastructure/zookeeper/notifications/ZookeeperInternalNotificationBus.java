package pl.allegro.tech.hermes.infrastructure.zookeeper.notifications;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.notifications.AdminCallback;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.SubscriptionCallback;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.ModelAwareZookeeperNotifyingCache;

public class ZookeeperInternalNotificationBus implements InternalNotificationsBus {

  private static final Logger logger =
      LoggerFactory.getLogger(ZookeeperInternalNotificationBus.class);

  private final ObjectMapper objectMapper;

  private final ModelAwareZookeeperNotifyingCache modelNotifyingCache;

  public ZookeeperInternalNotificationBus(
      ObjectMapper objectMapper, ModelAwareZookeeperNotifyingCache modelNotifyingCache) {
    this.objectMapper = objectMapper;
    this.modelNotifyingCache = modelNotifyingCache;
  }

  @Override
  public void registerSubscriptionCallback(SubscriptionCallback callback) {
    modelNotifyingCache.registerSubscriptionCallback(
        (e) -> {
          switch (e.getType()) {
            case CHILD_ADDED:
              readSilently(e.getData(), Subscription.class)
                  .ifPresent(callback::onSubscriptionCreated);
              break;
            case CHILD_UPDATED:
              readSilently(e.getData(), Subscription.class)
                  .ifPresent(callback::onSubscriptionChanged);
              break;
            case CHILD_REMOVED:
              readSilently(e.getData(), Subscription.class)
                  .ifPresent(callback::onSubscriptionRemoved);
              break;
            default:
              break;
          }
        });
  }

  @Override
  public void registerTopicCallback(TopicCallback callback) {
    modelNotifyingCache.registerTopicCallback(
        (e) -> {
          switch (e.getType()) {
            case CHILD_ADDED:
              readSilently(e.getData(), Topic.class).ifPresent(callback::onTopicCreated);
              break;
            case CHILD_UPDATED:
              readSilently(e.getData(), Topic.class).ifPresent(callback::onTopicChanged);
              break;
            case CHILD_REMOVED:
              readSilently(e.getData(), Topic.class).ifPresent(callback::onTopicRemoved);
              break;
            default:
              break;
          }
        });
  }

  @Override
  public void registerAdminCallback(AdminCallback callback) {
    // TODO we should move admin callbacks here in favor of AdminTool
  }

  private <T> Optional<T> readSilently(ChildData data, Class<T> clazz) {
    if (ArrayUtils.isEmpty(data.getData())) {
      logger.warn("No data at path {}", data.getPath());
      return empty();
    }
    try {
      return of(objectMapper.readValue(data.getData(), clazz));
    } catch (IOException exception) {
      logger.warn("Failed to parse object at path {}", data.getPath(), exception);
      return empty();
    }
  }
}
