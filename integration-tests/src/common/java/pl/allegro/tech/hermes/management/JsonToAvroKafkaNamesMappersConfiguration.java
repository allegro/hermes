package pl.allegro.tech.hermes.management;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import pl.allegro.tech.hermes.env.IntegrationTestKafkaNamesMapperFactory;
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.config.kafka.KafkaNamesMappers;
import pl.allegro.tech.hermes.management.config.kafka.MultipleDcKafkaNamesMappersFactory;

@Configuration
@EnableConfigurationProperties(KafkaClustersProperties.class)
public class JsonToAvroKafkaNamesMappersConfiguration
    implements MultipleDcKafkaNamesMappersFactory {

  @Bean
  @Primary
  @Profile("integration")
  KafkaNamesMappers testKafkaNameMappers(KafkaClustersProperties kafkaClustersProperties) {
    return createKafkaNamesMapper(
        kafkaClustersProperties,
        namespace -> new IntegrationTestKafkaNamesMapperFactory(namespace).create());
  }
}
