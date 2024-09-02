package pl.allegro.tech.hermes.consumers.config;

import com.google.common.base.Ticker;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@EnableConfigurationProperties(CommonConsumerProperties.class)
public class SubscriptionConfiguration {

  @Bean
  public SubscriptionIdProvider subscriptionIdProvider(
      CuratorFramework curatorFramework, ZookeeperPaths zookeeperPaths) {
    return new ZookeeperSubscriptionIdProvider(curatorFramework, zookeeperPaths);
  }

  @Bean
  public SubscriptionIds subscriptionIds(
      InternalNotificationsBus internalNotificationsBus,
      SubscriptionsCache subscriptionsCache,
      SubscriptionIdProvider subscriptionIdProvider,
      CommonConsumerProperties commonConsumerProperties) {
    NotificationAwareSubscriptionIdsCache cache =
        new NotificationAwareSubscriptionIdsCache(
            internalNotificationsBus,
            subscriptionsCache,
            subscriptionIdProvider,
            commonConsumerProperties.getSubscriptionIdsCacheRemovedExpireAfterAccess().toSeconds(),
            Ticker.systemTicker());
    cache.start();
    return cache;
  }

  @Bean
  public SubscriptionsCache subscriptionsCache(
      InternalNotificationsBus notificationsBus,
      GroupRepository groupRepository,
      TopicRepository topicRepository,
      SubscriptionRepository subscriptionRepository) {
    SubscriptionsCache cache =
        new NotificationsBasedSubscriptionCache(
            notificationsBus, groupRepository, topicRepository, subscriptionRepository);
    cache.start();
    return cache;
  }
}
