package pl.allegro.tech.hermes.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.domain.consistency.DcConsistencyService;
import pl.allegro.tech.hermes.management.domain.consistency.KafkaHermesConsistencyService;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

@Configuration
@EnableConfigurationProperties(ConsistencyCheckerProperties.class)
public class ConsistencyConfiguration {

  @Bean
  public KafkaHermesConsistencyService kafkaHermesConsistencyService(
      TopicService topicService,
      MultiDCAwareService multiDCAwareService,
      KafkaClustersProperties kafkaClustersProperties) {
    return new KafkaHermesConsistencyService(
        topicService,
        multiDCAwareService,
        kafkaClustersProperties.getDefaultNamespace(),
        kafkaClustersProperties.getNamespaceSeparator());
  }

  @Bean
  public DcConsistencyService dcConsistencyService(
      RepositoryManager repositoryManager,
      ObjectMapper objectMapper,
      ConsistencyCheckerProperties properties,
      MetricsFacade metricsFacade) {
    return new DcConsistencyService(repositoryManager, objectMapper, properties, metricsFacade);
  }
}
