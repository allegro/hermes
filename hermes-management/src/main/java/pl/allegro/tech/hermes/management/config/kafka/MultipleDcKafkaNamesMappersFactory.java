package pl.allegro.tech.hermes.management.config.kafka;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.function.Function;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;

/**
 * Factory for creating multiple Kafka names mappers per datacenter.
 *
 * @deprecated TODO: Remove this class after JsonToAvro topic migration feature will be removed from
 *     the codebase. We can simplify it and use a single KafkaNamesMapper across entire
 *     hermes-management instead of multiple mappers per DC.
 */
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
            .collect(
                toMap(
                    KafkaProperties::getClusterName,
                    c -> factoryFunction.apply(clustersProperties.getNamespace())));

    return new KafkaNamesMappers(mappers);
  }
}
