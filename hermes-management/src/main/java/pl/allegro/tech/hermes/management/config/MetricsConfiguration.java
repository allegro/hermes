package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionMetricsRepository;
import pl.allegro.tech.hermes.management.domain.topic.TopicMetricsRepository;
import pl.allegro.tech.hermes.management.infrastructure.metrics.HybridSubscriptionMetricsRepository;
import pl.allegro.tech.hermes.management.infrastructure.metrics.HybridTopicMetricsRepository;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringSubscriptionMetricsProvider;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringTopicMetricsProvider;
import pl.allegro.tech.hermes.management.infrastructure.metrics.SummedSharedCounter;

@Configuration
public class MetricsConfiguration {

  @Bean
  public TopicMetricsRepository topicMetricsRepository(
      MonitoringTopicMetricsProvider monitoringTopicMetricsProvider,
      SummedSharedCounter summedSharedCounter,
      ZookeeperPaths zookeeperPaths,
      SubscriptionRepository subscriptionRepository) {
    return new HybridTopicMetricsRepository(
        monitoringTopicMetricsProvider,
        summedSharedCounter,
        zookeeperPaths,
        subscriptionRepository);
  }

  @Bean
  public SubscriptionMetricsRepository subscriptionMetricsRepository(
      MonitoringSubscriptionMetricsProvider monitoringSubscriptionMetricsProvider,
      SummedSharedCounter summedSharedCounter,
      ZookeeperPaths zookeeperPaths,
      SubscriptionLagSource lagSource) {
    return new HybridSubscriptionMetricsRepository(
        monitoringSubscriptionMetricsProvider, summedSharedCounter, zookeeperPaths, lagSource);
  }
}
