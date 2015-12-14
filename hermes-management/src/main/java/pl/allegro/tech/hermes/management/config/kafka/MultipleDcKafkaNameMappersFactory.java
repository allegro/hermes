package pl.allegro.tech.hermes.management.config.kafka;

import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;

import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public interface MultipleDcKafkaNameMappersFactory {

    default KafkaNameMappers createDefaultKafkaNamesMapper(KafkaClustersProperties clustersProperties) {
        return createKafkaNamesMapper(clustersProperties, namespace -> new NamespaceKafkaNamesMapper(namespace));
    }

    default KafkaNameMappers createKafkaNamesMapper(KafkaClustersProperties clustersProperties, Function<String, KafkaNamesMapper> factoryFunction) {
        Map<String, KafkaNamesMapper> mappers = clustersProperties.getClusters().stream()
                .filter(c -> c.getNamespace().isEmpty())
                .collect(toMap(KafkaProperties::getClusterName,
                        kafkaProperties -> factoryFunction.apply(clustersProperties.getDefaultNamespace())));

        mappers.putAll(clustersProperties.getClusters().stream()
                .filter(c -> !c.getNamespace().isEmpty())
                .collect(toMap(KafkaProperties::getClusterName,
                        kafkaProperties -> factoryFunction.apply(kafkaProperties.getClusterName()))));

        return new KafkaNameMappers(mappers);
    }
}
