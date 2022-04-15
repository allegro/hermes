package pl.allegro.tech.hermes.consumers.config;

import com.google.common.base.Ticker;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.subscription.cache.NotificationsBasedSubscriptionCache;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.subscription.id.NotificationAwareSubscriptionIdsCache;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIdProvider;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.subscription.id.ZookeeperSubscriptionIdProvider;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

@Configuration
public class SubscriptionConfiguration {

    @Bean
    public SubscriptionIdProvider subscriptionIdProvider(CuratorFramework curatorFramework, ZookeeperPaths zookeeperPaths) {
        return new ZookeeperSubscriptionIdProvider(curatorFramework, zookeeperPaths);
    }

    @Bean
    public SubscriptionIds subscriptionIds(InternalNotificationsBus internalNotificationsBus,
                                           SubscriptionsCache subscriptionsCache,
                                           SubscriptionIdProvider subscriptionIdProvider,
                                           ConfigFactory configFactory) {
        long removedSubscriptionsExpireAfterAccessSeconds = configFactory.getLongProperty(Configs.CONSUMER_SUBSCRIPTION_IDS_CACHE_REMOVED_EXPIRE_AFTER_ACCESS_SECONDS);
        NotificationAwareSubscriptionIdsCache cache = new NotificationAwareSubscriptionIdsCache(
                internalNotificationsBus,
                subscriptionsCache,
                subscriptionIdProvider,
                removedSubscriptionsExpireAfterAccessSeconds,
                Ticker.systemTicker()
        );
        cache.start();
        return cache;
    }

    @Bean
    public SubscriptionsCache subscriptionsCache(InternalNotificationsBus notificationsBus,
                                                 GroupRepository groupRepository,
                                                 TopicRepository topicRepository,
                                                 SubscriptionRepository subscriptionRepository) {
        SubscriptionsCache cache = new NotificationsBasedSubscriptionCache(
                notificationsBus,
                groupRepository,
                topicRepository,
                subscriptionRepository
        );
        cache.start();
        return cache;
    }
}
