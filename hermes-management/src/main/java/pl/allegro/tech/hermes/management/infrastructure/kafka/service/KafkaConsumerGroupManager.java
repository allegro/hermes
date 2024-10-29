package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.management.config.kafka.KafkaProperties;
import pl.allegro.tech.hermes.management.domain.subscription.ConsumerGroupManager;

public class KafkaConsumerGroupManager implements ConsumerGroupManager {

  private final Logger logger = LoggerFactory.getLogger(KafkaConsumerGroupManager.class);

  private final KafkaNamesMapper kafkaNamesMapper;
  private final String clusterName;
  private final KafkaConsumerManager consumerManager;

  public KafkaConsumerGroupManager(
      KafkaNamesMapper kafkaNamesMapper,
      String clusterName,
      String brokerList,
      KafkaProperties kafkaProperties) {
    this.kafkaNamesMapper = kafkaNamesMapper;
    this.clusterName = clusterName;
    this.consumerManager = new KafkaConsumerManager(kafkaProperties, kafkaNamesMapper, brokerList);
  }

  @Override
  public void createConsumerGroup(Topic topic, Subscription subscription) {
    logger.info(
        "Creating consumer group for subscription {}, cluster: {}",
        subscription.getQualifiedName(),
        clusterName);

    KafkaConsumer<byte[], byte[]> kafkaConsumer =
        consumerManager.createConsumer(subscription.getQualifiedName());
    try {
      String kafkaTopicName = kafkaNamesMapper.toKafkaTopics(topic).getPrimary().name().asString();
      Set<TopicPartition> topicPartitions =
          kafkaConsumer.partitionsFor(kafkaTopicName).stream()
              .map(info -> new TopicPartition(info.topic(), info.partition()))
              .collect(toSet());

      logger.info("Received partitions: {}, cluster: {}", topicPartitions, clusterName);

      kafkaConsumer.assign(topicPartitions);

      Map<TopicPartition, OffsetAndMetadata> topicPartitionByOffset =
          topicPartitions.stream()
              .map(
                  topicPartition -> {
                    long offset = kafkaConsumer.position(topicPartition);
                    return ImmutablePair.of(topicPartition, new OffsetAndMetadata(offset));
                  })
              .collect(toMap(Pair::getKey, Pair::getValue));

      kafkaConsumer.commitSync(topicPartitionByOffset);
      kafkaConsumer.close();

      logger.info(
          "Successfully created consumer group for subscription {}, cluster: {}",
          subscription.getQualifiedName(),
          clusterName);
    } catch (Exception e) {
      logger.error(
          "Failed to create consumer group for subscription {}, cluster: {}",
          subscription.getQualifiedName(),
          clusterName,
          e);
    }
  }
}
