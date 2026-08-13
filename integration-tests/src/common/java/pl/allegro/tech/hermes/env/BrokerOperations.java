package pl.allegro.tech.hermes.env;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;

public class BrokerOperations {

  private static final Logger logger = LoggerFactory.getLogger(BrokerOperations.class);

  private static final int DEFAULT_PARTITIONS = 2;
  private static final int DEFAULT_REPLICATION_FACTOR = 1;

  private final AdminClient adminClient;

  private final KafkaNamesMapper kafkaNamesMapper;

  public BrokerOperations(String brokerList, String namespace) {
    this.adminClient = brokerAdminClient(brokerList);
    String namespaceSeparator = "_";
    this.kafkaNamesMapper = new JsonToAvroMigrationKafkaNamesMapper(namespace, namespaceSeparator);
  }

  public List<ConsumerGroupOffset> getTopicPartitionsOffsets(SubscriptionName subscriptionName) {
    ConsumerGroupId consumerGroupId = kafkaNamesMapper.toConsumerGroupId(subscriptionName);

    Map<TopicPartition, OffsetAndMetadata> currentOffsets = getTopicCurrentOffsets(consumerGroupId);
    Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
        getEndOffsets(new ArrayList<>(currentOffsets.keySet()));
    return currentOffsets.keySet().stream()
        .map(
            partition ->
                new ConsumerGroupOffset(
                    currentOffsets.get(partition).offset(), endOffsets.get(partition).offset()))
        .collect(Collectors.toList());
  }

  private Map<TopicPartition, OffsetAndMetadata> getTopicCurrentOffsets(
      ConsumerGroupId consumerGroupId) {
    try {
      return adminClient
          .listConsumerGroupOffsets(consumerGroupId.asString())
          .partitionsToOffsetAndMetadata()
          .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> getEndOffsets(
      List<TopicPartition> partitions) {
    try {
      ListOffsetsResult listOffsetsResult =
          adminClient.listOffsets(
              partitions.stream().collect(toMap(Function.identity(), p -> OffsetSpec.latest())));
      return listOffsetsResult.all().get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void createTopic(String topicName) {
    Topic topic = topic(topicName).build();
    kafkaNamesMapper.toKafkaTopics(topic).forEach(kafkaTopic -> createTopic(kafkaTopic.name()));
  }

  private void createTopic(KafkaTopicName topicName) {
    try {
      NewTopic topic =
          new NewTopic(
              topicName.asString(), DEFAULT_PARTITIONS, (short) DEFAULT_REPLICATION_FACTOR);
      adminClient.createTopics(singletonList(topic)).all().get(1, MINUTES);
    } catch (ExecutionException | TimeoutException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean topicExists(String topicName) {
    Topic topic = topic(topicName).build();
    return kafkaNamesMapper.toKafkaTopics(topic).allMatch(this::topicExists);
  }

  public String kafkaTopicName(Topic topic) {
    return kafkaNamesMapper.toKafkaTopics(topic).getPrimary().name().asString();
  }

  public void setTopicConfigs(String kafkaTopicName, Map<String, String> configs) {
    ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, kafkaTopicName);
    Collection<AlterConfigOp> operations =
        configs.entrySet().stream()
            .map(
                entry ->
                    new AlterConfigOp(
                        new ConfigEntry(entry.getKey(), entry.getValue()),
                        AlterConfigOp.OpType.SET))
            .toList();
    try {
      adminClient.incrementalAlterConfigs(Map.of(resource, operations)).all().get(1, MINUTES);
    } catch (ExecutionException | TimeoutException | InterruptedException e) {
      logger.warn("Failed to set topic configs for topic {}: {}", kafkaTopicName, e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public Map<String, String> readTopicConfigs(String kafkaTopicName) {
    ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, kafkaTopicName);
    try {
      Config config =
          adminClient.describeConfigs(List.of(resource)).all().get(1, MINUTES).get(resource);
      return config.entries().stream()
          .filter(entry -> entry.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG)
          .collect(toMap(ConfigEntry::name, ConfigEntry::value));
    } catch (ExecutionException | TimeoutException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public int getPartitionCount(String kafkaTopicName) {
    try {
      return adminClient
          .describeTopics(List.of(kafkaTopicName))
          .allTopicNames()
          .get(1, MINUTES)
          .get(kafkaTopicName)
          .partitions()
          .size();
    } catch (ExecutionException | TimeoutException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void deleteTopic(String kafkaTopicName) {
    try {
      adminClient.deleteTopics(List.of(kafkaTopicName)).all().get(1, MINUTES);
    } catch (ExecutionException | TimeoutException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean topicExists(KafkaTopic kafkaTopic) {
    try {
      return adminClient
          .listTopics()
          .names()
          .get(1, MINUTES)
          .contains(kafkaTopic.name().asString());
    } catch (ExecutionException | TimeoutException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private AdminClient brokerAdminClient(String brokerList) {
    Properties props = new Properties();
    props.put(BOOTSTRAP_SERVERS_CONFIG, brokerList);
    props.put(SECURITY_PROTOCOL_CONFIG, DEFAULT_SECURITY_PROTOCOL);
    props.put(REQUEST_TIMEOUT_MS_CONFIG, 10000);
    return AdminClient.create(props);
  }

  public static class ConsumerGroupOffset {
    private final long currentOffset;
    private final long endOffset;

    ConsumerGroupOffset(long currentOffset, long endOffset) {
      this.currentOffset = currentOffset;
      this.endOffset = endOffset;
    }

    public boolean movedToEnd() {
      return currentOffset == endOffset;
    }
  }
}
