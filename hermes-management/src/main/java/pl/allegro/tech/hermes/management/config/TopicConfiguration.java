package pl.allegro.tech.hermes.management.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionRemover;
import pl.allegro.tech.hermes.management.domain.topic.LoggingTopicService;
import pl.allegro.tech.hermes.management.domain.topic.TopicContentTypeMigrationService;
import pl.allegro.tech.hermes.management.domain.topic.TopicManagement;
import pl.allegro.tech.hermes.management.domain.topic.TopicMetricsRepository;
import pl.allegro.tech.hermes.management.domain.topic.TopicOwnerCache;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaService;
import pl.allegro.tech.hermes.management.domain.topic.validator.TopicValidator;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class TopicConfiguration {

  @Bean
  public TopicManagement topicManagement(
      MultiDCAwareService multiDCAwareService,
      TopicRepository topicRepository,
      GroupService groupService,
      TopicProperties topicProperties,
      SchemaService schemaService,
      TopicMetricsRepository metricRepository,
      TopicValidator topicValidator,
      TopicContentTypeMigrationService topicContentTypeMigrationService,
      Clock clock,
      Auditor auditor,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor,
      RepositoryManager repositoryManager,
      TopicOwnerCache topicOwnerCache,
      SubscriptionRemover subscriptionRemover) {
    TopicService topicService =
        new TopicService(
            multiDCAwareService,
            topicRepository,
            groupService,
            topicProperties,
            schemaService,
            metricRepository,
            topicValidator,
            topicContentTypeMigrationService,
            clock,
            auditor,
            multiDcExecutor,
            repositoryManager,
            topicOwnerCache,
            subscriptionRemover);
    return new LoggingTopicService(topicService);
  }

    @Bean
    public TopicOwnerCache topicOwnerCache(
            TopicRepository topicRepository, GroupService groupService, CacheProperties cacheProperties) {
        return new TopicOwnerCache(
                topicRepository, groupService, cacheProperties.getTopicOwnerRefreshRateInSeconds());
    }

    @Bean
    public TopicContentTypeMigrationService topicContentTypeMigrationService(
            SubscriptionRepository subscriptionRepository,
            MultiDCAwareService multiDCAwareService,
            Clock clock) {
        return new TopicContentTypeMigrationService(subscriptionRepository, multiDCAwareService, clock);
    }
}
