package pl.allegro.tech.hermes.frontend.cache.topic;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

import javax.inject.Inject;

public class TopicsCacheFactory implements Factory<TopicsCache> {

    private final InternalNotificationsBus internalNotificationsBus;

    private final GroupRepository groupRepository;

    private final TopicRepository topicRepository;
    private final HermesMetrics hermesMetrics;
    private final KafkaNamesMapper kafkaNamesMapper;

    @Inject
    public TopicsCacheFactory(InternalNotificationsBus internalNotificationsBus,
                              GroupRepository groupRepository,
                              TopicRepository topicRepository,
                              HermesMetrics hermesMetrics,
                              KafkaNamesMapper kafkaNamesMapper) {
        this.internalNotificationsBus = internalNotificationsBus;
        this.groupRepository = groupRepository;
        this.topicRepository = topicRepository;
        this.hermesMetrics = hermesMetrics;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    public TopicsCache provide() {
        TopicsCache cache = new NotificationBasedTopicsCache(internalNotificationsBus, groupRepository,
                topicRepository, hermesMetrics, kafkaNamesMapper);
        cache.start();
        return cache;
    }

    @Override
    public void dispose(TopicsCache instance) {
    }
}
