package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.infrastructure.tracker.NoOperationLogRepository;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

@Configuration
public class LogRepositoryConfiguration {

  @Bean
  @ConditionalOnMissingBean(LogRepository.class)
  public LogRepository logRepository() {
    return new NoOperationLogRepository();
  }
}
