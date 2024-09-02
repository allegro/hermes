package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
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
        createTopicConfig(topic.getRetentionTime().getDurationInMillis(), topicProperties);

    kafkaNamesMapper.toKafkaTopics(topic).stream()
        .map(
            k ->
                kafkaAdminClient.createTopics(
                    Collections.singletonList(
                        new NewTopic(
                                k.name().asString(),
                                topicProperties.getPartitions(),
                                (short) topicProperties.getReplicationFactor())
                            .configs(config))))
        .map(CreateTopicsResult::all)
        .forEach(this::waitForKafkaFuture);
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
        createTopicConfig(topic.getRetentionTime().getDurationInMillis(), topicProperties);
    KafkaTopics kafkaTopics = kafkaNamesMapper.toKafkaTopics(topic);

    if (isMigrationToNewKafkaTopic(kafkaTopics)) {
      KafkaFuture<Void> createTopicsFuture =
          kafkaAdminClient
              .createTopics(
                  Collections.singletonList(
                      new NewTopic(
                              kafkaTopics.getPrimary().name().asString(),
                              topicProperties.getPartitions(),
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
    return kafkaNamesMapper.toKafkaTopics(topic).allMatch(this::doesTopicExist);
  }

  private boolean isMigrationToNewKafkaTopic(KafkaTopics kafkaTopics) {
    return kafkaTopics.getSecondary().isPresent() && !doesTopicExist(kafkaTopics.getPrimary());
  }

  private boolean doesTopicExist(KafkaTopic topic) {
    KafkaFuture<Boolean> topicExistsFuture =
        kafkaAdminClient
            .listTopics()
            .names()
            .thenApply(names -> names.contains(topic.name().asString()));
    return waitForKafkaFuture(topicExistsFuture);
  }

  private void doUpdateTopic(KafkaTopic topic, Map<String, String> configMap) {
    ConfigResource topicConfigResource =
        new ConfigResource(ConfigResource.Type.TOPIC, topic.name().asString());

    Collection<ConfigEntry> configEntries =
        configMap.entrySet().stream()
            .map(entry -> new ConfigEntry(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

    Map<ConfigResource, Config> configUpdates = new HashMap<>();
    configUpdates.put(topicConfigResource, new Config(configEntries));

    KafkaFuture<Void> updateTopicFuture = kafkaAdminClient.alterConfigs(configUpdates).all();
    waitForKafkaFuture(updateTopicFuture);
  }

  private Map<String, String> createTopicConfig(
      long retentionPolicy, TopicProperties topicProperties) {
    Map<String, String> props = new HashMap<>();
    props.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionPolicy));
    props.put(
        TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG,
        Boolean.toString(topicProperties.isUncleanLeaderElectionEnabled()));
    props.put(
        TopicConfig.MAX_MESSAGE_BYTES_CONFIG, String.valueOf(topicProperties.getMaxMessageSize()));

    return props;
  }

  private <T> T waitForKafkaFuture(KafkaFuture<T> future) {
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new BrokersClusterCommunicationException(e);
    }
  }
}
