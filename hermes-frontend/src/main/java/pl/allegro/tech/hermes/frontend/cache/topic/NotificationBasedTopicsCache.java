package pl.allegro.tech.hermes.frontend.cache.topic;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NotificationBasedTopicsCache implements TopicCallback, TopicsCache {

    private final ConcurrentMap<String, Topic> topicCache = new ConcurrentHashMap<>();

    private final GroupRepository groupRepository;

    private final TopicRepository topicRepository;

    public NotificationBasedTopicsCache(InternalNotificationsBus notificationsBus,
                                        GroupRepository groupRepository,
                                        TopicRepository topicRepository) {
        this.groupRepository = groupRepository;
        this.topicRepository = topicRepository;
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

    @Override
    public void start() {
        for(String groupName : groupRepository.listGroupNames()) {
            for(Topic topic : topicRepository.listTopics(groupName)) {
                topicCache.put(topic.getQualifiedName(), topic);
            }
        }
    }
}
