package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.MemberDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ConsumerGroup;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.management.domain.message.RetransmissionService;
import pl.allegro.tech.hermes.management.domain.subscription.ConsumerGroupManager;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BrokersClusterService {

    private static final Logger logger = LoggerFactory.getLogger(BrokersClusterService.class);

    private final String clusterName;
    private final SingleMessageReader singleMessageReader;
    private final RetransmissionService retransmissionService;
    private final BrokerTopicManagement brokerTopicManagement;
    private final KafkaNamesMapper kafkaNamesMapper;
    private final OffsetsAvailableChecker offsetsAvailableChecker;
    private final ConsumerGroupsDescriber consumerGroupsDescriber;
    private final AdminClient adminClient;
    private final ConsumerGroupManager consumerGroupManager;

    public BrokersClusterService(String clusterName, SingleMessageReader singleMessageReader,
                                 RetransmissionService retransmissionService, BrokerTopicManagement brokerTopicManagement,
                                 KafkaNamesMapper kafkaNamesMapper, OffsetsAvailableChecker offsetsAvailableChecker,
                                 LogEndOffsetChecker logEndOffsetChecker, AdminClient adminClient,
                                 ConsumerGroupManager consumerGroupManager) {
        this.clusterName = clusterName;
        this.singleMessageReader = singleMessageReader;
        this.retransmissionService = retransmissionService;
        this.brokerTopicManagement = brokerTopicManagement;
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.offsetsAvailableChecker = offsetsAvailableChecker;
        this.consumerGroupsDescriber = new ConsumerGroupsDescriber(
                kafkaNamesMapper,
                adminClient,
                logEndOffsetChecker,
                clusterName
        );
        this.adminClient = adminClient;
        this.consumerGroupManager = consumerGroupManager;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void manageTopic(Consumer<BrokerTopicManagement> manageFunction) {
        manageFunction.accept(brokerTopicManagement);
    }

    public String readMessageFromPrimary(Topic topic, Integer partition, Long offset) {
        return singleMessageReader.readMessageAsJson(topic, kafkaNamesMapper.toKafkaTopics(topic).getPrimary(), partition, offset);
    }

    public List<PartitionOffset> indicateOffsetChange(Topic topic, String subscriptionName, Long timestamp, boolean dryRun) {
        return retransmissionService.indicateOffsetChange(topic, subscriptionName, clusterName, timestamp, dryRun);
    }

    public boolean areOffsetsAvailableOnAllKafkaTopics(Topic topic) {
        return kafkaNamesMapper.toKafkaTopics(topic).allMatch(offsetsAvailableChecker::areOffsetsAvailable);
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

    public boolean allSubscriptionsHaveConsumersAssigned(Topic topic, List<Subscription> subscriptions) {
        List<String> consumerGroupsForSubscriptions = subscriptions.stream()
                .map(sub -> kafkaNamesMapper.toConsumerGroupId(sub.getQualifiedName()).asString())
                .collect(Collectors.toList());

        try {
            int requiredTotalNumberOfAssignments = numberOfPartitionsForTopic(topic) * subscriptions.size();
            return numberOfAssignmentsForConsumersGroups(consumerGroupsForSubscriptions) == requiredTotalNumberOfAssignments;
        } catch (Exception e) {
            logger.error("Failed to check assignments for topic " + topic.getQualifiedName() + " subscriptions", e);
            return false;
        }
    }

    public void createConsumerGroup(Topic topic, Subscription subscription) {
        consumerGroupManager.createConsumerGroup(topic, subscription);
    }

    public Optional<ConsumerGroup> describeConsumerGroup(Topic topic, String subscriptionName) {
        return consumerGroupsDescriber.describeConsumerGroup(topic, subscriptionName);
    }

    private int numberOfAssignmentsForConsumersGroups(List<String> consumerGroupsIds) throws ExecutionException, InterruptedException {
        Collection<ConsumerGroupDescription> consumerGroupsDescriptions =
                adminClient.describeConsumerGroups(consumerGroupsIds).all().get().values();
        Stream<MemberDescription> memberDescriptions = consumerGroupsDescriptions.stream().flatMap(desc -> desc.members().stream());
        return memberDescriptions.flatMap(memberDescription -> memberDescription.assignment().topicPartitions().stream())
                .collect(Collectors.toList()).size();
    }

    private int numberOfPartitionsForTopic(Topic topic) throws ExecutionException, InterruptedException {
        List<String> kafkaTopicsNames = kafkaNamesMapper.toKafkaTopics(topic).stream()
                .map(kafkaTopic -> kafkaTopic.name().asString())
                .collect(Collectors.toList());

        return adminClient.describeTopics(kafkaTopicsNames).all().get().values().stream()
                .map(v -> v.partitions().size())
                .reduce(0, Integer::sum);
    }
}
