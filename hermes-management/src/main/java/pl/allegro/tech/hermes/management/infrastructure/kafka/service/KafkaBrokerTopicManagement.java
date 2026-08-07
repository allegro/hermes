package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.BrokersClusterCommunicationException;

public class KafkaBrokerTopicManagement implements BrokerTopicManagement {

  private final TopicProperties topicProperties;

  private final AdminClient kafkaAdminClient;

  private final KafkaNamesMapper kafkaNamesMapper;

  private final String datacenterName;

  private static final Logger logger = LoggerFactory.getLogger(KafkaBrokerTopicManagement.class);

  public KafkaBrokerTopicManagement(
      TopicProperties topicProperties,
      AdminClient kafkaAdminClient,
      KafkaNamesMapper kafkaNamesMapper,
      String datacenterName) {
    this.topicProperties = topicProperties;
    this.kafkaAdminClient = kafkaAdminClient;
    this.kafkaNamesMapper = kafkaNamesMapper;
    this.datacenterName = datacenterName;
  }

  @Override
  public void createTopic(Topic topic) {
    Map<String, String> config =
        OwnedTopicConfig.desiredConfig(
            topic.getRetentionTime().getDurationInMillis(), topicProperties);

    kafkaNamesMapper.toKafkaTopics(topic).stream()
        .forEach(kafkaTopic -> createTopic(kafkaTopic, config));
  }

  @Override
  public void createTopic(Topic topic, KafkaTopic kafkaTopic) {
    createTopic(
        kafkaTopic,
        OwnedTopicConfig.desiredConfig(
            topic.getRetentionTime().getDurationInMillis(), topicProperties));
  }

  @Override
  public void removeTopic(Topic topic) {
    kafkaNamesMapper.toKafkaTopics(topic).stream()
        .map(k -> kafkaAdminClient.deleteTopics(Collections.singletonList(k.name().asString())))
        .map(DeleteTopicsResult::all)
        .forEach(
            future -> {
              logger.info("Removing topic: {} from Kafka dc: {}", topic, datacenterName);
              long start = System.currentTimeMillis();
              waitForKafkaFuture(future);
              logger.info(
                  "Removed topic: {} from Kafka dc: {} in {} ms",
                  topic,
                  datacenterName,
                  System.currentTimeMillis() - start);
            });
  }

  @Override
  public void updateTopic(Topic topic) {
    Map<String, String> config =
        OwnedTopicConfig.desiredConfig(
            topic.getRetentionTime().getDurationInMillis(), topicProperties);
    KafkaTopics kafkaTopics = kafkaNamesMapper.toKafkaTopics(topic);

    if (isMigrationToNewKafkaTopic(kafkaTopics)) {
      KafkaFuture<Void> createTopicsFuture =
          kafkaAdminClient
              .createTopics(
                  Collections.singletonList(
                      new NewTopic(
                              kafkaTopics.getPrimary().name().asString(),
                              getPartitionsForDatacenter(datacenterName),
                              (short) topicProperties.getReplicationFactor())
                          .configs(config)))
              .all();
      waitForKafkaFuture(createTopicsFuture);
    } else {
      doUpdateTopic(kafkaTopics.getPrimary(), config);
    }

    kafkaTopics.getSecondary().ifPresent(secondary -> doUpdateTopic(secondary, config));
  }

  @Override
  public boolean topicExists(Topic topic) {
    return kafkaNamesMapper.toKafkaTopics(topic).allMatch(this::topicExists);
  }

  @Override
  public boolean topicExists(KafkaTopic topic) {
    KafkaFuture<Boolean> topicExistsFuture =
        kafkaAdminClient
            .listTopics()
            .names()
            .thenApply(names -> names.contains(topic.name().asString()));
    return waitForKafkaFuture(topicExistsFuture);
  }

  @Override
  public Set<String> listTopicNames() {
    return waitForKafkaFuture(kafkaAdminClient.listTopics().names());
  }

  @Override
  public Map<KafkaTopic, Map<String, String>> readTopicConfigs(Collection<KafkaTopic> kafkaTopics) {
    Map<ConfigResource, KafkaTopic> topicsByResource =
        kafkaTopics.stream()
            .collect(
                Collectors.toMap(
                    topic -> new ConfigResource(ConfigResource.Type.TOPIC, topic.name().asString()),
                    Function.identity()));
    Map<ConfigResource, Config> configs =
        waitForKafkaFuture(kafkaAdminClient.describeConfigs(topicsByResource.keySet()).all());

    return configs.entrySet().stream()
        .collect(
            Collectors.toMap(
                entry -> topicsByResource.get(entry.getKey()),
                entry -> readOwnedDynamicConfig(entry.getValue())));
  }

  private boolean isMigrationToNewKafkaTopic(KafkaTopics kafkaTopics) {
    return kafkaTopics.getSecondary().isPresent() && !topicExists(kafkaTopics.getPrimary());
  }

  private void createTopic(KafkaTopic kafkaTopic, Map<String, String> config) {
    CreateTopicsResult result =
        kafkaAdminClient.createTopics(
            Collections.singletonList(
                new NewTopic(
                        kafkaTopic.name().asString(),
                        getPartitionsForDatacenter(datacenterName),
                        (short) topicProperties.getReplicationFactor())
                    .configs(config)));
    waitForKafkaFuture(result.all());
  }

  @Override
  public void updateTopicConfig(KafkaTopic topic, Map<String, String> configMap) {
    doUpdateTopic(topic, configMap);
  }

  private void doUpdateTopic(KafkaTopic topic, Map<String, String> configMap) {
    ConfigResource topicConfigResource =
        new ConfigResource(ConfigResource.Type.TOPIC, topic.name().asString());

    Collection<AlterConfigOp> configEntries =
        configMap.entrySet().stream()
            .map(
                entry ->
                    new AlterConfigOp(
                        new ConfigEntry(entry.getKey(), entry.getValue()),
                        AlterConfigOp.OpType.SET))
            .collect(Collectors.toList());

    Map<ConfigResource, Collection<AlterConfigOp>> configUpdates = new HashMap<>();
    configUpdates.put(topicConfigResource, configEntries);

    KafkaFuture<Void> updateTopicFuture =
        kafkaAdminClient.incrementalAlterConfigs(configUpdates).all();
    waitForKafkaFuture(updateTopicFuture);
  }

  private Map<String, String> readOwnedDynamicConfig(Config config) {
    return OwnedTopicConfig.KEYS.stream()
        .map(config::get)
        .filter(
            entry ->
                entry != null && entry.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG)
        .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value));
  }

  private <T> T waitForKafkaFuture(KafkaFuture<T> future) {
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new BrokersClusterCommunicationException(e);
    }
  }

  private int getPartitionsForDatacenter(String datacenterName) {
    return topicProperties
        .getPartitionsPerDc()
        .getOrDefault(datacenterName, topicProperties.getPartitions());
  }
}
