package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ConsumerGroup;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.management.domain.message.RetransmissionService;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupManager;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MovingSubscriptionOffsetsValidationException;

public class BrokersClusterService {

  private static final Logger logger = LoggerFactory.getLogger(BrokersClusterService.class);

  private final String datacenter;
  private final String clusterName;
  private final SingleMessageReader singleMessageReader;
  private final RetransmissionService retransmissionService;
  private final BrokerTopicManagement brokerTopicManagement;
  private final KafkaNamesMapper kafkaNamesMapper;
  private final OffsetsAvailableChecker offsetsAvailableChecker;
  private final ConsumerGroupsDescriber consumerGroupsDescriber;
  private final AdminClient adminClient;
  private final ConsumerGroupManager consumerGroupManager;

  public BrokersClusterService(
      String datacenter,
      String clusterName,
      SingleMessageReader singleMessageReader,
      RetransmissionService retransmissionService,
      BrokerTopicManagement brokerTopicManagement,
      KafkaNamesMapper kafkaNamesMapper,
      OffsetsAvailableChecker offsetsAvailableChecker,
      LogEndOffsetChecker logEndOffsetChecker,
      AdminClient adminClient,
      ConsumerGroupManager consumerGroupManager) {
    this.datacenter = datacenter;
    this.clusterName = clusterName;
    this.singleMessageReader = singleMessageReader;
    this.retransmissionService = retransmissionService;
    this.brokerTopicManagement = brokerTopicManagement;
    this.kafkaNamesMapper = kafkaNamesMapper;
    this.offsetsAvailableChecker = offsetsAvailableChecker;
    this.consumerGroupsDescriber =
        new ConsumerGroupsDescriber(
            kafkaNamesMapper, adminClient, logEndOffsetChecker, clusterName);
    this.adminClient = adminClient;
    this.consumerGroupManager = consumerGroupManager;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public void manageTopic(Consumer<BrokerTopicManagement> manageFunction) {
    manageFunction.accept(brokerTopicManagement);
  }

  public String readMessageFromPrimary(Topic topic, Integer partition, Long offset) {
    return singleMessageReader.readMessageAsJson(
        topic, kafkaNamesMapper.toKafkaTopics(topic).getPrimary(), partition, offset);
  }

  public void indicateOffsetChange(
      Topic topic, String subscriptionName, List<PartitionOffset> partitionOffsets) {
    retransmissionService.indicateOffsetChange(
        topic, subscriptionName, clusterName, partitionOffsets);
  }

  public List<PartitionOffset> fetchTopicOffsetsAt(Topic topic, Long timestamp) {
    return retransmissionService.fetchTopicOffsetsAt(topic, timestamp);
  }

  public boolean areOffsetsAvailableOnAllKafkaTopics(Topic topic) {
    return kafkaNamesMapper
        .toKafkaTopics(topic)
        .allMatch(offsetsAvailableChecker::areOffsetsAvailable);
  }

  public boolean topicExists(Topic topic) {
    return brokerTopicManagement.topicExists(topic);
  }

  public List<String> listTopicsFromCluster() {
    try {
      return new ArrayList<>(adminClient.listTopics().names().get());
    } catch (ExecutionException | InterruptedException e) {
      logger.error("Failed to list topics names", e);
      return Collections.emptyList();
    }
  }

  public void removeTopicByName(String topicName) {
    adminClient.deleteTopics(Collections.singletonList(topicName));
  }

  public boolean areOffsetsMoved(Topic topic, String subscriptionName) {
    return retransmissionService.areOffsetsMoved(topic, subscriptionName, clusterName);
  }

  public boolean allSubscriptionsHaveConsumersAssigned(
      Topic topic, List<Subscription> subscriptions) {
    List<String> consumerGroupsForSubscriptions =
        subscriptions.stream()
            .map(sub -> kafkaNamesMapper.toConsumerGroupId(sub.getQualifiedName()).asString())
            .collect(Collectors.toList());

    try {
      int requiredTotalNumberOfAssignments =
          numberOfPartitionsForTopic(topic) * subscriptions.size();
      return numberOfAssignmentsForConsumersGroups(consumerGroupsForSubscriptions)
          == requiredTotalNumberOfAssignments;
    } catch (Exception e) {
      logger.error(
          "Failed to check assignments for topic " + topic.getQualifiedName() + " subscriptions",
          e);
      return false;
    }
  }

  public void createConsumerGroup(Topic topic, Subscription subscription) {
    consumerGroupManager.createConsumerGroup(topic, subscription);
  }

  public void deleteConsumerGroup(SubscriptionName subscriptionName) {
    consumerGroupManager.deleteConsumerGroup(subscriptionName);
  }

  public Optional<ConsumerGroup> describeConsumerGroup(Topic topic, String subscriptionName) {
    return consumerGroupsDescriber.describeConsumerGroup(topic, subscriptionName);
  }

  public void moveOffsets(SubscriptionName subscription, List<PartitionOffset> offsets) {
    ConsumerGroupId consumerGroupId = kafkaNamesMapper.toConsumerGroupId(subscription);
    Map<TopicPartition, OffsetAndMetadata> offsetAndMetadata = buildOffsetsMetadata(offsets);
    adminClient.alterConsumerGroupOffsets(consumerGroupId.asString(), offsetAndMetadata).all();

    logger.info(
        "Successfully moved offsets for subscription {} and consumer group {} to {}",
        subscription.getQualifiedName(),
        kafkaNamesMapper.toConsumerGroupId(subscription),
        offsetAndMetadata.toString());
  }

  private int numberOfAssignmentsForConsumersGroups(List<String> consumerGroupsIds)
      throws ExecutionException, InterruptedException {
    Collection<ConsumerGroupDescription> consumerGroupsDescriptions =
        adminClient.describeConsumerGroups(consumerGroupsIds).all().get().values();
    Stream<MemberDescription> memberDescriptions =
        consumerGroupsDescriptions.stream().flatMap(desc -> desc.members().stream());
    return memberDescriptions
        .flatMap(memberDescription -> memberDescription.assignment().topicPartitions().stream())
        .collect(Collectors.toList())
        .size();
  }

  public void validateIfOffsetsCanBeMovedByConsumers(Topic topic, SubscriptionName subscription) {
    describeConsumerGroup(topic, subscription.getName())
        .ifPresentOrElse(
            group -> {
              if (!group.isStable()) {
                String s =
                    format(
                        "Consumer group %s for subscription %s is not stable.",
                        group.getGroupId(), subscription.getQualifiedName());
                throw new MovingSubscriptionOffsetsValidationException(s);
              }
            },
            () -> {
              String s =
                  format(
                      "No consumer group for subscription %s exists.",
                      subscription.getQualifiedName());
              throw new MovingSubscriptionOffsetsValidationException(s);
            });
  }

  public void validateIfOffsetsCanBeMoved(Topic topic, SubscriptionName subscription) {
    describeConsumerGroup(topic, subscription.getName())
        .ifPresentOrElse(
            group -> {
              if (!group.isEmpty()) {
                String s =
                    format(
                        "Consumer group %s for subscription %s has still active members.",
                        group.getGroupId(), subscription.getQualifiedName());
                throw new MovingSubscriptionOffsetsValidationException(s);
              }
            },
            () -> {
              String s =
                  format(
                      "No consumer group for subscription %s exists.",
                      subscription.getQualifiedName());
              throw new MovingSubscriptionOffsetsValidationException(s);
            });
  }

  private int numberOfPartitionsForTopic(Topic topic)
      throws ExecutionException, InterruptedException {
    List<String> kafkaTopicsNames =
        kafkaNamesMapper.toKafkaTopics(topic).stream()
            .map(kafkaTopic -> kafkaTopic.name().asString())
            .collect(Collectors.toList());

    return adminClient.describeTopics(kafkaTopicsNames).all().get().values().stream()
        .map(v -> v.partitions().size())
        .reduce(0, Integer::sum);
  }

  private Set<TopicPartition> getTopicPartitions(
      KafkaConsumer<byte[], byte[]> consumer, String kafkaTopicName) {
    return consumer.partitionsFor(kafkaTopicName).stream()
        .map(info -> new TopicPartition(info.topic(), info.partition()))
        .collect(toSet());
  }

  private Map<TopicPartition, OffsetAndMetadata> buildOffsetsMetadata(
      List<PartitionOffset> offsets) {
    return offsets.stream()
        .map(
            offset ->
                ImmutablePair.of(
                    new TopicPartition(offset.getTopic().asString(), offset.getPartition()),
                    new OffsetAndMetadata(offset.getOffset())))
        .collect(toMap(Pair::getKey, Pair::getValue));
  }
}
