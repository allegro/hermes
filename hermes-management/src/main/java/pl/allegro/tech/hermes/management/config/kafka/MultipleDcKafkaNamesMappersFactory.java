package pl.allegro.tech.hermes.management.config.kafka;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.function.Function;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;

public interface MultipleDcKafkaNamesMappersFactory {

  default KafkaNamesMappers createDefaultKafkaNamesMapper(
      KafkaClustersProperties clustersProperties) {
    return createKafkaNamesMapper(
        clustersProperties,
        namespace ->
            new NamespaceKafkaNamesMapper(namespace, clustersProperties.getNamespaceSeparator()));
  }

  default KafkaNamesMappers createKafkaNamesMapper(
      KafkaClustersProperties clustersProperties,
      Function<String, KafkaNamesMapper> factoryFunction) {
    Map<String, KafkaNamesMapper> mappers =
        clustersProperties.getClusters().stream()
            .filter(c -> c.getNamespace().isEmpty())
            .collect(
                toMap(
                    KafkaProperties::getQualifiedClusterName,
                    kafkaProperties ->
                        factoryFunction.apply(clustersProperties.getDefaultNamespace())));

    mappers.putAll(
        clustersProperties.getClusters().stream()
            .filter(c -> !c.getNamespace().isEmpty())
            .collect(
                toMap(
                    KafkaProperties::getQualifiedClusterName,
                    kafkaProperties -> factoryFunction.apply(kafkaProperties.getNamespace()))));

    return new KafkaNamesMappers(mappers);
  }
}
