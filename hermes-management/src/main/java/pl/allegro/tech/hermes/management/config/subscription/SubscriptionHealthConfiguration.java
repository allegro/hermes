package pl.allegro.tech.hermes.management.config.subscription;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Clock;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.config.subscription.consumergroup.ConsumerGroupCleanUpProperties;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager;
import pl.allegro.tech.hermes.management.domain.subscription.LoggingSubscriptionService;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionManagement;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionMetricsRepository;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionOwnerCache;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionRemover;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthChecker;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.DisabledIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.LaggingIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.MalfunctioningIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.ReceivingMalformedMessagesIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.TimingOutIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.UnreachableIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.validator.SubscriptionValidator;
import pl.allegro.tech.hermes.management.domain.topic.TopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

@Configuration
@EnableConfigurationProperties({SubscriptionHealthProperties.class})
public class SubscriptionHealthConfiguration {
  private static final DisabledIndicator DISABLED_INDICATOR = new DisabledIndicator();

  private final SubscriptionHealthProperties subscriptionHealthProperties;

  public SubscriptionHealthConfiguration(
      SubscriptionHealthProperties subscriptionHealthProperties) {
    this.subscriptionHealthProperties = subscriptionHealthProperties;
  }

  @Bean
  public SubscriptionHealthProblemIndicator laggingIndicator() {
    if (subscriptionHealthProperties.isLaggingIndicatorEnabled()) {
      return new LaggingIndicator(subscriptionHealthProperties.getMaxLagInSeconds());
    }
    return DISABLED_INDICATOR;
  }

  @Bean
  public SubscriptionHealthProblemIndicator unreachableIndicator() {
    if (subscriptionHealthProperties.isUnreachableIndicatorEnabled()) {
      return new UnreachableIndicator(
          subscriptionHealthProperties.getMaxOtherErrorsRatio(),
          subscriptionHealthProperties.getMinSubscriptionRateForReliableMetrics());
    }
    return DISABLED_INDICATOR;
  }

  @Bean
  public SubscriptionHealthProblemIndicator timingOutIndicator() {
    if (subscriptionHealthProperties.isTimingOutIndicatorEnabled()) {
      return new TimingOutIndicator(
          subscriptionHealthProperties.getMaxTimeoutsRatio(),
          subscriptionHealthProperties.getMinSubscriptionRateForReliableMetrics());
    }
    return DISABLED_INDICATOR;
  }

  @Bean
  public SubscriptionHealthProblemIndicator malfunctioningIndicator() {
    if (subscriptionHealthProperties.isMalfunctioningIndicatorEnabled()) {
      return new MalfunctioningIndicator(
          subscriptionHealthProperties.getMax5xxErrorsRatio(),
          subscriptionHealthProperties.getMinSubscriptionRateForReliableMetrics());
    }
    return DISABLED_INDICATOR;
  }

  @Bean
  public SubscriptionHealthProblemIndicator receivingMalformedMessagesIndicator() {
    if (subscriptionHealthProperties.isReceivingMalformedMessagesIndicatorEnabled()) {
      return new ReceivingMalformedMessagesIndicator(
          subscriptionHealthProperties.getMax4xxErrorsRatio(),
          subscriptionHealthProperties.getMinSubscriptionRateForReliableMetrics());
    }
    return DISABLED_INDICATOR;
  }

  @Bean
  public SubscriptionManagement subscriptionManagement(
      SubscriptionRepository subscriptionRepository,
      SubscriptionOwnerCache subscriptionOwnerCache,
      TopicManagement topicManagement,
      SubscriptionMetricsRepository metricsRepository,
      SubscriptionHealthChecker subscriptionHealthChecker,
      LogRepository logRepository,
      SubscriptionValidator subscriptionValidator,
      Auditor auditor,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor,
      MultiDCAwareService multiDCAwareService,
      RepositoryManager repositoryManager,
      SubscriptionHealthProperties subscriptionHealthProperties,
      SubscriptionRemover subscriptionRemover) {
    SubscriptionService subscriptionService =
        new SubscriptionService(
            subscriptionRepository,
            subscriptionOwnerCache,
            topicManagement,
            metricsRepository,
            subscriptionHealthChecker,
            logRepository,
            subscriptionValidator,
            auditor,
            multiDcExecutor,
            multiDCAwareService,
            repositoryManager,
            Executors.newFixedThreadPool(
                subscriptionHealthProperties.getThreads(),
                new ThreadFactoryBuilder()
                    .setNameFormat("subscription-health-check-executor-%d")
                    .build()),
            subscriptionHealthProperties.getTimeoutMillis(),
            subscriptionRemover);
    return new LoggingSubscriptionService(subscriptionService);
  }

  @Bean
  public SubscriptionRemover subscriptionRemover(
      Auditor auditor,
      MultiDatacenterRepositoryCommandExecutor multiDatacenterRepositoryCommandExecutor,
      SubscriptionOwnerCache subscriptionOwnerCache,
      SubscriptionRepository subscriptionRepository,
      ConsumerGroupCleanUpProperties consumerGroupCleanUpProperties,
      Clock clock) {
    return new SubscriptionRemover(
        auditor,
        multiDatacenterRepositoryCommandExecutor,
        subscriptionOwnerCache,
        subscriptionRepository,
        consumerGroupCleanUpProperties.isEnabled(),
        clock);
  }
}
