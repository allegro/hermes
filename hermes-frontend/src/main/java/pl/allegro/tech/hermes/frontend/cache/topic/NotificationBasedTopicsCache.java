package pl.allegro.tech.hermes.frontend.cache.topic;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NotificationBasedTopicsCache implements TopicCallback, TopicsCache {

    private final ConcurrentMap<String, CachedTopic> topicCache = new ConcurrentHashMap<>();

    private final GroupRepository groupRepository;
    private final TopicRepository topicRepository;
    private final HermesMetrics hermesMetrics;
    private final KafkaNamesMapper kafkaNamesMapper;

    public NotificationBasedTopicsCache(InternalNotificationsBus notificationsBus,
                                        GroupRepository groupRepository,
                                        TopicRepository topicRepository,
                                        HermesMetrics hermesMetrics,
                                        KafkaNamesMapper kafkaNamesMapper) {
        this.groupRepository = groupRepository;
        this.topicRepository = topicRepository;
        this.hermesMetrics = hermesMetrics;
        this.kafkaNamesMapper = kafkaNamesMapper;
        notificationsBus.registerTopicCallback(this);
    }

    @Override
    public void onTopicCreated(Topic topic) {
        topicCache.put(topic.getName().qualifiedName(), cachedTopic(topic));
    }

    @Override
    public void onTopicRemoved(Topic topic) {
        topicCache.remove(topic.getName().qualifiedName(), topic);
    }

    @Override
    public void onTopicChanged(Topic topic) {
        topicCache.put(topic.getName().qualifiedName(), cachedTopic(topic));
    }

    @Override
    public Optional<CachedTopic> getTopic(String qualifiedTopicName) {
        return Optional.ofNullable(topicCache.get(qualifiedTopicName));
    }

    @Override
    public void start() {
        for(String groupName : groupRepository.listGroupNames()) {
            for(Topic topic : topicRepository.listTopics(groupName)) {
                topicCache.put(topic.getQualifiedName(), cachedTopic(topic));
            }
        }
    }

    private CachedTopic cachedTopic(Topic topic) {
        return new CachedTopic(topic, hermesMetrics, kafkaNamesMapper.toKafkaTopics(topic));
    }
}
