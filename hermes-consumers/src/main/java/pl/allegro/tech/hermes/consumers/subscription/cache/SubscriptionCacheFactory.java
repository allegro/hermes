package pl.allegro.tech.hermes.consumers.subscription.cache;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

import javax.inject.Inject;

public class SubscriptionCacheFactory implements Factory<SubscriptionsCache> {

    private final InternalNotificationsBus notificationsBus;

    private final GroupRepository groupRepository;

    private final TopicRepository topicRepository;

    private final SubscriptionRepository subscriptionRepository;

    @Inject
    public SubscriptionCacheFactory(InternalNotificationsBus notificationsBus,
                                    GroupRepository groupRepository,
                                    TopicRepository topicRepository,
                                    SubscriptionRepository subscriptionRepository) {
        this.notificationsBus = notificationsBus;
        this.groupRepository = groupRepository;
        this.topicRepository = topicRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public SubscriptionsCache provide() {
        SubscriptionsCache cache = new NotificationsBasedSubscriptionCache(
                notificationsBus, groupRepository, topicRepository, subscriptionRepository
        );
        cache.start();
        return cache;
    }

    @Override
    public void dispose(SubscriptionsCache instance) {

    }
}
