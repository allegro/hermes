package pl.allegro.tech.hermes.management.infrastructure.kafka;

import static java.util.stream.Collectors.toList;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ConsumerGroup;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.retransmit.RetransmitCommand;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;
import pl.allegro.tech.hermes.management.domain.topic.UnableToMoveOffsetsException;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.BrokersClusterService;

public class MultiDCAwareService {

  private static final Logger logger = LoggerFactory.getLogger(MultiDCAwareService.class);

  private final List<BrokersClusterService> clusters;
  private final Clock clock;
  private final Duration intervalBetweenCheckingIfOffsetsMoved;
  private final Duration offsetsMovedTimeout;
  private final MultiDatacenterRepositoryCommandExecutor multiDcExecutor;

  public MultiDCAwareService(
      List<BrokersClusterService> clusters,
      Clock clock,
      Duration intervalBetweenCheckingIfOffsetsMoved,
      Duration offsetsMovedTimeout,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor) {
    this.clusters = clusters;
    this.clock = clock;
    this.intervalBetweenCheckingIfOffsetsMoved = intervalBetweenCheckingIfOffsetsMoved;
    this.offsetsMovedTimeout = offsetsMovedTimeout;
    this.multiDcExecutor = multiDcExecutor;
  }

  public void manageTopic(Consumer<BrokerTopicManagement> manageFunction) {
    clusters.forEach(kafkaService -> kafkaService.manageTopic(manageFunction));
  }

  public String readMessageFromPrimary(
      String clusterName, Topic topic, Integer partition, Long offset) {
    return clusters.stream()
        .filter(cluster -> clusterName.equals(cluster.getClusterName()))
        .findFirst()
        .orElseThrow(() -> new BrokersClusterNotFoundException(clusterName))
        .readMessageFromPrimary(topic, partition, offset);
  }

  public MultiDCOffsetChangeSummary fetchTopicOffsetsAt(Topic topic, Long timestamp) {
    MultiDCOffsetChangeSummary multiDCOffsetChangeSummary = new MultiDCOffsetChangeSummary();

    clusters.forEach(
        cluster ->
            multiDCOffsetChangeSummary.addPartitionOffsetList(
                cluster.getClusterName(), cluster.fetchTopicOffsetsAt(topic, timestamp)));

    return multiDCOffsetChangeSummary;
  }

  public void moveOffsets(
      Topic topic,
      String subscriptionName,
      Map<String, List<PartitionOffset>> brokerPartitionOffsets) {
    clusters.forEach(
        cluster ->
            cluster.validateIfOffsetsCanBeMoved(
                topic, new SubscriptionName(subscriptionName, topic.getName())));

    clusters.forEach(
        cluster ->
            cluster.moveOffsets(
                new SubscriptionName(subscriptionName, topic.getName()),
                brokerPartitionOffsets.getOrDefault(
                    cluster.getClusterName(), Collections.emptyList())));
  }

  public void moveOffsetsForActiveConsumers(
      Topic topic,
      String subscriptionName,
      Map<String, List<PartitionOffset>> brokerPartitionOffsets,
      RequestUser requester) {
    clusters.forEach(
        cluster ->
            cluster.validateIfOffsetsCanBeMovedByConsumers(
                topic, new SubscriptionName(subscriptionName, topic.getName())));

    clusters.forEach(
        cluster ->
            Optional.ofNullable(brokerPartitionOffsets.get(cluster.getClusterName()))
                .ifPresent(
                    offsets -> cluster.indicateOffsetChange(topic, subscriptionName, offsets)));

    logger.info(
        "Starting moving offsets for subscription {}. Requested by {}. Retransmission offsets: {}",
        topic.getQualifiedName() + "$" + subscriptionName,
        requester.getUsername(),
        brokerPartitionOffsets);

    multiDcExecutor.executeByUser(
        new RetransmitCommand(new SubscriptionName(subscriptionName, topic.getName())), requester);
    clusters.forEach(clusters -> waitUntilOffsetsAreMoved(topic, subscriptionName));

    logger.info(
        "Successfully moved offsets for retransmission of subscription {}. Requested by user: {}. Retransmission offsets: {}",
        topic.getQualifiedName() + "$" + subscriptionName,
        requester.getUsername(),
        brokerPartitionOffsets);
  }

  public boolean areOffsetsAvailableOnAllKafkaTopics(Topic topic) {
    return clusters.stream()
        .allMatch(cluster -> cluster.areOffsetsAvailableOnAllKafkaTopics(topic));
  }

  public boolean topicExists(Topic topic) {
    return clusters.stream()
        .allMatch(brokersClusterService -> brokersClusterService.topicExists(topic));
  }

  public Set<String> listTopicFromAllDC() {
    return clusters.stream()
        .map(BrokersClusterService::listTopicsFromCluster)
        .flatMap(Collection::stream)
        .collect(Collectors.toCollection(HashSet::new));
  }

  public void removeTopicByName(String topicName) {
    clusters.forEach(brokersClusterService -> brokersClusterService.removeTopicByName(topicName));
  }

  public void createConsumerGroups(Topic topic, Subscription subscription) {
    clusters.forEach(clusterService -> clusterService.createConsumerGroup(topic, subscription));
  }

  private void waitUntilOffsetsAreMoved(Topic topic, String subscriptionName) {
    Instant abortAttemptsInstant = clock.instant().plus(offsetsMovedTimeout);

    while (!areOffsetsMoved(topic, subscriptionName)) {
      if (clock.instant().isAfter(abortAttemptsInstant)) {
        logger.error(
            "Not all offsets related to hermes subscription {}${} were moved.",
            topic.getQualifiedName(),
            subscriptionName);
        throw new UnableToMoveOffsetsException(topic, subscriptionName);
      }
      logger.debug(
          "Not all offsets related to hermes subscription {} were moved, will retry",
          topic.getQualifiedName());

      sleep(intervalBetweenCheckingIfOffsetsMoved);
    }
  }

  private boolean areOffsetsMoved(Topic topic, String subscriptionName) {
    return clusters.stream().allMatch(cluster -> cluster.areOffsetsMoved(topic, subscriptionName));
  }

  private void sleep(Duration sleepDuration) {
    try {
      Thread.sleep(sleepDuration.toMillis());
    } catch (InterruptedException e) {
      throw new InternalProcessingException(e);
    }
  }

  public boolean allSubscriptionsHaveConsumersAssigned(
      Topic topic, List<Subscription> subscriptions) {
    return clusters.stream()
        .allMatch(
            brokersClusterService ->
                brokersClusterService.allSubscriptionsHaveConsumersAssigned(topic, subscriptions));
  }

  public List<ConsumerGroup> describeConsumerGroups(Topic topic, String subscriptionName) {
    return clusters.stream()
        .map(
            brokersClusterService ->
                brokersClusterService.describeConsumerGroup(topic, subscriptionName))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  public void deleteConsumerGroupForDatacenter(
      SubscriptionName subscriptionName, String datacenter) {
    clusters.stream()
        .filter(cluster -> cluster.getDatacenter().equals(datacenter))
        .findFirst()
        .ifPresentOrElse(
            cluster -> cluster.deleteConsumerGroup(subscriptionName),
            () -> {
              throw new BrokersClusterNotFoundException(datacenter);
            });
  }
}
