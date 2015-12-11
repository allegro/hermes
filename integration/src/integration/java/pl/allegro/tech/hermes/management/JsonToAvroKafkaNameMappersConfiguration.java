package pl.allegro.tech.hermes.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.config.kafka.KafkaNameMappers;
import pl.allegro.tech.hermes.management.config.kafka.KafkaProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
public class JsonToAvroKafkaNameMappersConfiguration {

    @Autowired
    KafkaClustersProperties kafkaClustersProperties;

    @Bean
    KafkaNameMappers kafkaNameMappers() {
        Map<String, KafkaNamesMapper> mappers = new HashMap<>();

        kafkaClustersProperties.getClusters().forEach(
                kafkaProperties -> mappers.put(kafkaProperties.getClusterName(), kafkaProperties.getNamespace().isEmpty() ?
                    new JsonToAvroMigrationKafkaNamesMapper(kafkaClustersProperties.getDefaultNamespace()) :
                    new JsonToAvroMigrationKafkaNamesMapper(kafkaProperties.getNamespace())));

        return new KafkaNameMappers(mappers);
    }
}
