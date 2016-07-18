package pl.allegro.tech.hermes.frontend.cache.topic;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

import javax.inject.Inject;

public class TopicsCacheFactory implements Factory<TopicsCache> {

    private final InternalNotificationsBus internalNotificationsBus;

    private final GroupRepository groupRepository;

    private final TopicRepository topicRepository;

    @Inject
    public TopicsCacheFactory(InternalNotificationsBus internalNotificationsBus,
                              GroupRepository groupRepository,
                              TopicRepository topicRepository) {
        this.internalNotificationsBus = internalNotificationsBus;
        this.groupRepository = groupRepository;
        this.topicRepository = topicRepository;
    }

    @Override
    public TopicsCache provide() {
        TopicsCache cache = new NotificationBasedTopicsCache(internalNotificationsBus, groupRepository, topicRepository);
        cache.start();
        return cache;
    }

    @Override
    public void dispose(TopicsCache instance) {
    }
}
