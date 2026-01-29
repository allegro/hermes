package pl.allegro.tech.hermes.management.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.topic.TopicContentTypeMigrationService;
import pl.allegro.tech.hermes.management.domain.topic.TopicOwnerCache;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class TopicConfiguration {

  @Bean
  public TopicOwnerCache topicOwnerCache(
      TopicRepository topicRepository,
      GroupService groupService,
      CacheProperties cacheProperties) {
    return new TopicOwnerCache(
        topicRepository, groupService, cacheProperties.getTopicOwnerRefreshRateInSeconds());
  }

  @Bean
  public TopicContentTypeMigrationService topicContentTypeMigrationService(
      SubscriptionRepository subscriptionRepository,
      MultiDCAwareService multiDCAwareService,
      Clock clock) {
    return new TopicContentTypeMigrationService(
        subscriptionRepository, multiDCAwareService, clock);
  }
}
