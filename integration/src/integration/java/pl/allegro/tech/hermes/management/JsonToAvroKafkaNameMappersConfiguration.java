package pl.allegro.tech.hermes.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper;
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.config.kafka.KafkaNameMappers;
import pl.allegro.tech.hermes.management.config.kafka.MultipleDcKafkaNameMappersFactory;

@Configuration
public class JsonToAvroKafkaNameMappersConfiguration implements MultipleDcKafkaNameMappersFactory {

    @Autowired
    KafkaClustersProperties kafkaClustersProperties;

    @Bean
    KafkaNameMappers kafkaNameMappers() {
        return createKafkaNamesMapper(kafkaClustersProperties, namespace -> new JsonToAvroMigrationKafkaNamesMapper(namespace));
    }
}
