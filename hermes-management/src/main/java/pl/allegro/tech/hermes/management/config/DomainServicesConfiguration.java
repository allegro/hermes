package pl.allegro.tech.hermes.management.config;

import java.time.Clock;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.CredentialsRepository;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.credentials.CredentialsService;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.group.GroupValidator;
import pl.allegro.tech.hermes.management.domain.oauth.OAuthProviderService;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionRemover;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthChecker;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;
import pl.allegro.tech.hermes.management.domain.topic.TopicContentTypeMigrationService;
import pl.allegro.tech.hermes.management.domain.topic.TopicMetricsRepository;
import pl.allegro.tech.hermes.management.domain.topic.TopicOwnerCache;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaService;
import pl.allegro.tech.hermes.management.domain.topic.validator.TopicValidator;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidatorProvider;
import pl.allegro.tech.hermes.schema.RawSchemaClient;

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
      RawSchemaClient rawSchemaClient,
      SchemaValidatorProvider validatorProvider,
      TopicProperties topicProperties) {
    return new SchemaService(rawSchemaClient, validatorProvider, topicProperties.isRemoveSchema());
  }

  @Bean
  public CredentialsService credentialsService(
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor,
      CredentialsRepository credentialsRepository) {
    return new CredentialsService(multiDcExecutor, credentialsRepository);
  }

  @Bean
  public OAuthProviderService oAuthProviderService(
      OAuthProviderRepository repository,
      ApiPreconditions preconditions,
      Auditor auditor,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor) {
    return new OAuthProviderService(repository, preconditions, auditor, multiDcExecutor);
  }

  @Bean
  public SubscriptionHealthChecker subscriptionHealthChecker(
      List<SubscriptionHealthProblemIndicator> problemIndicators) {
    return new SubscriptionHealthChecker(problemIndicators);
  }
}
