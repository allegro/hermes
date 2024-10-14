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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;

public class BrokerOperations {

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
