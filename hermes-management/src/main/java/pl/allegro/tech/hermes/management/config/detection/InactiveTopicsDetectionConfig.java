package pl.allegro.tech.hermes.management.config.detection;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsDetectionJob;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsDetectionService;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsNotifier;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsRepository;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsStorageService;
import pl.allegro.tech.hermes.management.domain.detection.LastPublishedMessageMetricsRepository;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.detection.InactiveTopicsDetectionScheduler;
import pl.allegro.tech.hermes.management.infrastructure.detection.ZookeeperLastPublishedMessageMetricsRepository;
import pl.allegro.tech.hermes.management.infrastructure.leader.ManagementLeadership;
import pl.allegro.tech.hermes.management.infrastructure.metrics.SummedSharedCounter;

@Configuration
@EnableConfigurationProperties(InactiveTopicsDetectionProperties.class)
@EnableScheduling
public class InactiveTopicsDetectionConfig {

  @Bean
  public LastPublishedMessageMetricsRepository lastPublishedMessageMetricsRepository(
      SummedSharedCounter summedSharedCounter, ZookeeperPaths zookeeperPaths) {
    return new ZookeeperLastPublishedMessageMetricsRepository(summedSharedCounter, zookeeperPaths);
  }

  @Bean
  public InactiveTopicsDetectionService inactiveTopicsDetectionService(
      LastPublishedMessageMetricsRepository metricsRepository,
      InactiveTopicsDetectionProperties properties,
      Clock clock) {
    return new InactiveTopicsDetectionService(metricsRepository, properties, clock);
  }

  @Bean
  public InactiveTopicsStorageService inactiveTopicsStorageService(
      InactiveTopicsRepository inactiveTopicsRepository,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor) {
    return new InactiveTopicsStorageService(inactiveTopicsRepository, multiDcExecutor);
  }

  @Bean
  public InactiveTopicsDetectionJob inactiveTopicsDetectionJob(
      TopicService topicService,
      InactiveTopicsStorageService inactiveTopicsStorageService,
      InactiveTopicsDetectionService inactiveTopicsDetectionService,
      Optional<InactiveTopicsNotifier> notifier,
      InactiveTopicsDetectionProperties properties,
      Clock clock,
      MeterRegistry meterRegistry) {
    return new InactiveTopicsDetectionJob(
        topicService,
        inactiveTopicsStorageService,
        inactiveTopicsDetectionService,
        notifier,
        properties,
        clock,
        meterRegistry);
  }

  @ConditionalOnProperty(
      prefix = "detection.inactive-topics",
      value = "enabled",
      havingValue = "true")
  @Bean
  InactiveTopicsDetectionScheduler inactiveTopicsDetectionScheduler(
      InactiveTopicsDetectionJob job, ManagementLeadership leader) {
    return new InactiveTopicsDetectionScheduler(job, leader);
  }
}
