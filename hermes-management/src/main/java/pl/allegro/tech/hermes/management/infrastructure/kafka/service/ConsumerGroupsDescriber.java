package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ConsumerGroup;
import pl.allegro.tech.hermes.api.ConsumerGroupMember;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.management.infrastructure.kafka.BrokersClusterCommunicationException;

class ConsumerGroupsDescriber {

  private static final Logger logger = LoggerFactory.getLogger(ConsumerGroupsDescriber.class);

  private final KafkaNamesMapper kafkaNamesMapper;
  private final AdminClient adminClient;
  private final LogEndOffsetChecker logEndOffsetChecker;
  private final String clusterName;

  ConsumerGroupsDescriber(
      KafkaNamesMapper kafkaNamesMapper,
      AdminClient adminClient,
      LogEndOffsetChecker logEndOffsetChecker,
      String clusterName) {
    this.kafkaNamesMapper = kafkaNamesMapper;
    this.adminClient = adminClient;
    this.logEndOffsetChecker = logEndOffsetChecker;
    this.clusterName = clusterName;
  }

  Optional<ConsumerGroup> describeConsumerGroup(Topic topic, String subscriptionName) {
    ConsumerGroupId consumerGroupId =
        kafkaNamesMapper.toConsumerGroupId(new SubscriptionName(subscriptionName, topic.getName()));
    KafkaTopics kafkaTopics = kafkaNamesMapper.toKafkaTopics(topic);
    try {
      return describeConsumerGroup(consumerGroupId, kafkaTopics);
    } catch (Exception e) {
      logger.error("Failed to describe group with id: {}", consumerGroupId.asString(), e);
      throw new BrokersClusterCommunicationException(e);
    }
  }

  private Optional<ConsumerGroup> describeConsumerGroup(
      ConsumerGroupId consumerGroupId, KafkaTopics kafkaTopics)
      throws ExecutionException, InterruptedException {
    Map<KafkaTopicName, ContentType> kafkaTopicContentTypes =
        kafkaTopics.stream().collect(toMap(KafkaTopic::name, KafkaTopic::contentType));
    Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsets =
        adminClient
            .listConsumerGroupOffsets(consumerGroupId.asString())
            .partitionsToOffsetAndMetadata()
            .get();
    Optional<ConsumerGroupDescription> description =
        adminClient
            .describeConsumerGroups(Collections.singletonList(consumerGroupId.asString()))
            .all()
            .get()
            .values()
            .stream()
            .findFirst();

    return description
        .map(d -> d.state() != ConsumerGroupState.DEAD ? d : null)
        .map(d -> getKafkaConsumerGroup(topicPartitionOffsets, kafkaTopicContentTypes, d));
  }

  private ConsumerGroup getKafkaConsumerGroup(
      Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsets,
      Map<KafkaTopicName, ContentType> kafkaTopicContentTypes,
      ConsumerGroupDescription description) {
    Set<ConsumerGroupMember> groupMembers =
        description.members().stream()
            .map(
                member ->
                    getKafkaConsumerGroupMember(
                        topicPartitionOffsets, kafkaTopicContentTypes, member))
            .collect(toSet());

    return new ConsumerGroup(
        clusterName, description.groupId(), description.state().toString(), groupMembers);
  }

  private ConsumerGroupMember getKafkaConsumerGroupMember(
      Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsets,
      Map<KafkaTopicName, ContentType> kafkaTopicContentTypes,
      MemberDescription member) {
    Set<pl.allegro.tech.hermes.api.TopicPartition> kafkaTopicPartitions =
        member.assignment().topicPartitions().stream()
            .map(
                topicPartition -> {
                  Optional<OffsetAndMetadata> offset =
                      Optional.ofNullable(topicPartitionOffsets.get(topicPartition));
                  return new pl.allegro.tech.hermes.api.TopicPartition(
                      topicPartition.partition(),
                      topicPartition.topic(),
                      offset.map(OffsetAndMetadata::offset).orElse(0L),
                      logEndOffsetChecker.check(topicPartition),
                      offset.map(OffsetAndMetadata::metadata).orElse(""),
                      kafkaTopicContentTypes.get(KafkaTopicName.valueOf(topicPartition.topic())));
                })
            .collect(toSet());
    return new ConsumerGroupMember(
        member.consumerId(), member.clientId(), toHostName(member.host()), kafkaTopicPartitions);
  }

  private static String toHostName(String inetAddressStringRepresentation) {
    String[] parts = inetAddressStringRepresentation.split("/");
    String ip = parts[parts.length - 1];
    try {
      InetAddress addr = InetAddress.getByName(ip);
      return addr.getHostName();
    } catch (UnknownHostException e) {
      return inetAddressStringRepresentation;
    }
  }
}
