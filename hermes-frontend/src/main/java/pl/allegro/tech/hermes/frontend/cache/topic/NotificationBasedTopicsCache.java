package pl.allegro.tech.hermes.frontend.cache.topic;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NotificationBasedTopicsCache implements TopicCallback, TopicsCache {

    private final ConcurrentMap<String, Topic> topicCache = new ConcurrentHashMap<>();

    @Inject
    public NotificationBasedTopicsCache(InternalNotificationsBus notificationsBus) {
        notificationsBus.registerTopicCallback(this);
    }

    @Override
    public void onTopicCreated(Topic topic) {
        topicCache.put(topic.getName().qualifiedName(), topic);
    }

    @Override
    public void onTopicRemoved(Topic topic) {
        topicCache.remove(topic.getName().qualifiedName(), topic);
    }

    @Override
    public void onTopicChanged(Topic topic) {
        topicCache.put(topic.getName().qualifiedName(), topic);
    }

    @Override
    public Optional<Topic> getTopic(String topicName) {
        return Optional.ofNullable(topicCache.get(topicName));
    }
}
