package pl.allegro.tech.hermes.management.infrastructure.monitoring;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class MonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    private final KafkaNamesMapper kafkaNamesMapper;
    private final AdminClient adminClient;
    private final String clusterName;

    public MonitoringService(KafkaNamesMapper kafkaNamesMapper, AdminClient adminClient, String clusterName) {
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.adminClient = adminClient;
        this.clusterName = clusterName;
    }

    public boolean checkIfAllPartitionsAreAssigned(Topic topic, String subscriptionName) {
        KafkaTopics kafkaTopics = kafkaNamesMapper.toKafkaTopics(topic);
        ConsumerGroupId consumerGroupId = kafkaNamesMapper.toConsumerGroupId(new SubscriptionName(subscriptionName, topic.getName()));
        try {
            return checkIfAllPartitionsAreAssigned(consumerGroupId, kafkaTopics);
        } catch (Exception e) {
            logger.error("Failed to describe group with id: {}", consumerGroupId.asString(), e);
            return true;
        }
    }

    private boolean checkIfAllPartitionsAreAssigned(ConsumerGroupId consumerGroupId,
                                                       KafkaTopics kafkaTopics) throws ExecutionException, InterruptedException {
        Optional<ConsumerGroupDescription> consumerGroupDescription = getConsumerGroupDescription(consumerGroupId);

        if (consumerGroupDescription.isEmpty()) {
            logger.info("Monitoring. Cannot get consumer group description about: {}", consumerGroupId.asString());
            return true;
        }
        if (isConsumerGroupRebalancing(consumerGroupDescription.get())) {
            logger.info("Monitoring. Consumer group is rebalancing: {}", consumerGroupId.asString());
            return true;
        }

        int topicPartitions = getTopicPartitions(kafkaTopics);
        int consumerGroupPartitions = getConsumerGroupPartitions(consumerGroupDescription.get());

        if (topicPartitions != consumerGroupPartitions) {
            logger.error("Unassigned partitions in consumer groups for subscription {} in cluster {}. "
                            + "Expected number of assigned partitions: {}, but was {}.",
                            consumerGroupId.asString(), clusterName,
                            topicPartitions, consumerGroupPartitions);
            return false;
        }
        return true;
    }

    private Optional<ConsumerGroupDescription> getConsumerGroupDescription(ConsumerGroupId consumerGroupId)
            throws InterruptedException, ExecutionException {
        return adminClient
                .describeConsumerGroups(Collections.singletonList(consumerGroupId.asString()))
                .all()
                .get()
                .values()
                .stream()
                .findFirst();
    }

    private int getTopicPartitions(KafkaTopics kafkaTopics) throws InterruptedException, ExecutionException {
        KafkaFuture<TopicDescription> topicDescription = describeTopic(kafkaTopics.getPrimary().name().asString());
        int topicPartitions = topicDescription.get().partitions().size();
        if (kafkaTopics.getSecondary().isPresent()) {
            topicPartitions += describeTopic(kafkaTopics.getSecondary().get().name().asString()).get().partitions().size();
        }
        return topicPartitions;
    }

    private static int getConsumerGroupPartitions(ConsumerGroupDescription consumerGroupDescription) {
        return consumerGroupDescription.members().stream()
                .mapToInt(member -> member.assignment().topicPartitions().size()).sum();
    }

    private boolean isConsumerGroupRebalancing(ConsumerGroupDescription description) {
        return description.state().equals(ConsumerGroupState.PREPARING_REBALANCE)
                || description.state().equals(ConsumerGroupState.COMPLETING_REBALANCE);
    }

    private KafkaFuture<TopicDescription> describeTopic(String topic) {
        return adminClient.describeTopics(Collections.singletonList(topic)).all()
                .thenApply(topicsMap -> topicsMap.get(topic));
    }
}