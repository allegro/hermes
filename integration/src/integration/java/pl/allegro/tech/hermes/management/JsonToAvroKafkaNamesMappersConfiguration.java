package pl.allegro.tech.hermes.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.integration.env.IntegrationTestKafkaNamesMapperFactory;
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.config.kafka.KafkaNamesMappers;
import pl.allegro.tech.hermes.management.config.kafka.MultipleDcKafkaNamesMappersFactory;

@Configuration
public class JsonToAvroKafkaNamesMappersConfiguration implements MultipleDcKafkaNamesMappersFactory {

    @Autowired
    KafkaClustersProperties kafkaClustersProperties;

    @Bean
    KafkaNamesMappers kafkaNameMappers() {
        return createKafkaNamesMapper(kafkaClustersProperties, namespace ->
                new IntegrationTestKafkaNamesMapperFactory(namespace).create());
    }
}
