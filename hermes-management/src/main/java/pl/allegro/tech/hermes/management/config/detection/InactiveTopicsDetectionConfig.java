package pl.allegro.tech.hermes.management.config.detection;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsDetectionJob;
import pl.allegro.tech.hermes.management.infrastructure.detection.InactiveTopicsDetectionLeader;
import pl.allegro.tech.hermes.management.infrastructure.detection.InactiveTopicsDetectionScheduler;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager;

@Configuration
@EnableConfigurationProperties(InactiveTopicsDetectionProperties.class)
@EnableScheduling
public class InactiveTopicsDetectionConfig {
  @ConditionalOnProperty(
      prefix = "detection.inactive-topics",
      value = "enabled",
      havingValue = "true")
  @Bean
  InactiveTopicsDetectionLeader inactiveTopicsDetectionLeader(
      ZookeeperClientManager zookeeperClientManager,
      InactiveTopicsDetectionProperties properties,
      ZookeeperPaths zookeeperPaths) {
    return new InactiveTopicsDetectionLeader(zookeeperClientManager, properties, zookeeperPaths);
  }

  @ConditionalOnProperty(
      prefix = "detection.inactive-topics",
      value = "enabled",
      havingValue = "true")
  @Bean
  InactiveTopicsDetectionScheduler inactiveTopicsDetectionScheduler(
      InactiveTopicsDetectionJob job, InactiveTopicsDetectionLeader leader) {
    return new InactiveTopicsDetectionScheduler(job, leader);
  }
}
