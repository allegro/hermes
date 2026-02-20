package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.AvroSchemaValidator;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidatorProvider;

@Configuration
@EnableConfigurationProperties(TopicProperties.class)
public class SchemaValidatorConfiguration {

  @Bean
  public AvroSchemaValidator avroSchemaValidator(TopicProperties topicProperties) {
    return new AvroSchemaValidator(topicProperties);
  }

  @Bean
  public SchemaValidatorProvider schemaValidatorProvider(AvroSchemaValidator avroSchemaValidator) {
    return new SchemaValidatorProvider(avroSchemaValidator);
  }
}
