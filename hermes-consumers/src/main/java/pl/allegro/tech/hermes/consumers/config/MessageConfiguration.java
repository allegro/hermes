package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;

@Configuration
@EnableConfigurationProperties(CommonConsumerProperties.class)
public class MessageConfiguration {

  @Bean
  public UndeliveredMessageLogPersister undeliveredMessageLogPersister(
      UndeliveredMessageLog undeliveredMessageLog,
      CommonConsumerProperties commonConsumerProperties) {
    return new UndeliveredMessageLogPersister(
        undeliveredMessageLog, commonConsumerProperties.getUndeliveredMessageLogPersistPeriod());
  }
}
