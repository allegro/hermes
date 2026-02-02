package pl.allegro.tech.hermes.management.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.credentials.CredentialsService;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.group.GroupValidator;
import pl.allegro.tech.hermes.management.domain.oauth.OAuthProviderService;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionRemover;
import pl.allegro.tech.hermes.management.domain.topic.TopicContentTypeMigrationService;
import pl.allegro.tech.hermes.management.domain.topic.TopicMetricsRepository;
import pl.allegro.tech.hermes.management.domain.topic.TopicOwnerCache;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaService;
import pl.allegro.tech.hermes.management.domain.topic.validator.TopicValidator;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

/**
 * Configuration for core domain services.
 * These services contain business logic and should not have Spring annotations.
 */
@Configuration
@EnableConfigurationProperties(TopicProperties.class)
public class DomainServicesConfiguration {

  @Bean
  public GroupService groupService(
      GroupRepository groupRepository,
      Auditor auditor,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor,
      GroupValidator validator) {
    return new GroupService(groupRepository, auditor, multiDcExecutor, validator);
  }

  @Bean
  public TopicService topicService(
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
    return new TopicService(
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
  }

  @Bean
  public SchemaService schemaService(
      pl.allegro.tech.hermes.schema.RawSchemaClient rawSchemaClient,
      pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidatorProvider validatorProvider,
      TopicProperties topicProperties) {
    return new SchemaService(rawSchemaClient, validatorProvider, topicProperties);
  }

  @Bean
  public CredentialsService credentialsService(
      ZookeeperPaths paths,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor,
      pl.allegro.tech.hermes.domain.CredentialsRepository credentialsRepository) {
    return new CredentialsService(paths, multiDcExecutor, credentialsRepository);
  }

  @Bean
  public OAuthProviderService oAuthProviderService(
      pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository repository,
      pl.allegro.tech.hermes.management.api.validator.ApiPreconditions preconditions,
      Auditor auditor,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor) {
    return new OAuthProviderService(repository, preconditions, auditor, multiDcExecutor);
  }

  @Bean
  public pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthChecker subscriptionHealthChecker(
      java.util.List<pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator> problemIndicators) {
    return new pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthChecker(problemIndicators);
  }

  // TODO: TopicContentTypeMigrationService temporarily uses @Component due to complex dependencies
  /*
  @Bean
  public pl.allegro.tech.hermes.management.domain.topic.TopicContentTypeMigrationService topicContentTypeMigrationService(...) {
    return new pl.allegro.tech.hermes.management.domain.topic.TopicContentTypeMigrationService(...);
  }
  */

  // Note: SearchService, SearchPredicateFactory, and SearchCache are now in SearchConfiguration

  // TODO: The following beans have been commented out temporarily as they need correct constructor parameters
  // They still use @Component and can be migrated in a future phase

  /*
  @Bean
  public TopicContentTypeMigrationService topicContentTypeMigrationService(...) {
    return new TopicContentTypeMigrationService(...);
  }

  @Bean
  public CredentialsService credentialsService(...) {
    return new CredentialsService(...);
  }

  @Bean
  public OAuthProviderService oAuthProviderService(...) {
    return new OAuthProviderService(...);
  }

  @Bean
  public SubscriptionHealthChecker subscriptionHealthChecker(...) {
    return new SubscriptionHealthChecker(...);
  }

  @Bean
  public OwnerIdValidator ownerIdValidator(...) {
    return new OwnerIdValidator(...);
  }
  */
}
