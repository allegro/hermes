package pl.allegro.tech.hermes.api;

import java.util.List;

public record InconsistentKafkaTopic(
    String qualifiedTopicName,
    String kafkaTopicName,
    String clusterName,
    boolean existsOnBroker,
    List<KafkaTopicConfigDiff> configDiffs) {}
