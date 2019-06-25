package pl.allegro.tech.hermes.infrastructure.zookeeper.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.ArrayUtils;
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

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

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
                    readSilently(e.getData(), Subscription.class).ifPresent(sub -> callback.onSubscriptionCreated(sub));
                    break;
                case CHILD_UPDATED:
                    readSilently(e.getData(), Subscription.class).ifPresent(sub -> callback.onSubscriptionChanged(sub));
                    break;
                case CHILD_REMOVED:
                    readSilently(e.getData(), Subscription.class).ifPresent(sub -> callback.onSubscriptionRemoved(sub));
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
                    readSilently(e.getData(), Topic.class).ifPresent(topic -> callback.onTopicCreated(topic));
                    break;
                case CHILD_UPDATED:
                    readSilently(e.getData(), Topic.class).ifPresent(topic -> callback.onTopicChanged(topic));
                    break;
                case CHILD_REMOVED:
                    readSilently(e.getData(), Topic.class).ifPresent(topic -> callback.onTopicRemoved(topic));
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
