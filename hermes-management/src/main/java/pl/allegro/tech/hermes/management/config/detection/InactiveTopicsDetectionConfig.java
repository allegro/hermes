package pl.allegro.tech.hermes.management.config.detection;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsDetectionJob;
import pl.allegro.tech.hermes.management.infrastructure.detection.InactiveTopicsDetectionScheduler;
import pl.allegro.tech.hermes.management.infrastructure.leader.ManagementLeadership;

@Configuration
@EnableConfigurationProperties(InactiveTopicsDetectionProperties.class)
@EnableScheduling
public class InactiveTopicsDetectionConfig {
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
