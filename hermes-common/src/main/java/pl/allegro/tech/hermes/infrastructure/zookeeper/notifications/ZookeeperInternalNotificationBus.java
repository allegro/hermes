package pl.allegro.tech.hermes.infrastructure.zookeeper.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.notifications.AdminCallback;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.SubscriptionCallback;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.ModelAwareZookeeperNotifyingCache;

import javax.inject.Inject;
import java.io.IOException;

public class ZookeeperInternalNotificationBus implements InternalNotificationsBus {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperInternalNotificationBus.class);

    private final ObjectMapper objectMapper;

    private final ModelAwareZookeeperNotifyingCache modelNotifyingCache;

    @Inject
    public ZookeeperInternalNotificationBus(ObjectMapper objectMapper, ModelAwareZookeeperNotifyingCache modelNotifyingCache) {
        this.objectMapper = objectMapper;
        this.modelNotifyingCache = modelNotifyingCache;
    }

    @Override
    public void registerSubscriptionCallback(SubscriptionCallback callback) {
        modelNotifyingCache.registerSubscriptionCallback((e) -> {
            switch (e.getType()) {
                case CHILD_ADDED:
                    callback.onSubscriptionCreated(readSilently(e.getData().getPath(), e.getData().getData(), Subscription.class));
                    break;
                case CHILD_UPDATED:
                    callback.onSubscriptionChanged(readSilently(e.getData().getPath(), e.getData().getData(), Subscription.class));
                    break;
                case CHILD_REMOVED:
                    callback.onSubscriptionRemoved(readSilently(e.getData().getPath(), e.getData().getData(), Subscription.class));
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public void registerTopicCallback(TopicCallback callback) {
        modelNotifyingCache.registerTopicCallback((e) -> {
            switch (e.getType()) {
                case CHILD_ADDED:
                    callback.onTopicCreated(readSilently(e.getData().getPath(), e.getData().getData(), Topic.class));
                    break;
                case CHILD_UPDATED:
                    callback.onTopicChanged(readSilently(e.getData().getPath(), e.getData().getData(), Topic.class));
                    break;
                case CHILD_REMOVED:
                    callback.onTopicRemoved(readSilently(e.getData().getPath(), e.getData().getData(), Topic.class));
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public void registerAdminCallback(AdminCallback callback) {
        // TODO noop
    }

    private <T> T readSilently(String path, byte[] data, Class<T> clazz) {
        try {
            return objectMapper.readValue(data, clazz);
        } catch (IOException exception) {
            logger.warn("Failed to parse object at path {}", path, exception);
            return null;
        }
    }
}
