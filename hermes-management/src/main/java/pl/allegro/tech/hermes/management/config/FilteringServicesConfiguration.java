package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.management.domain.filtering.FilteringService;
import pl.allegro.tech.hermes.management.domain.topic.TopicManagement;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

@Configuration
public class FilteringServicesConfiguration {

  @Bean
  public FilteringService filteringService(
      FilterChainFactory filterChainFactory,
      SchemaRepository schemaRepository,
      TopicManagement topicManagement,
      JsonAvroConverter jsonAvroConverter) {
    return new FilteringService(
        filterChainFactory, schemaRepository, topicManagement, jsonAvroConverter);
  }
}
