package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ConsumerGroup;
import pl.allegro.tech.hermes.api.ConsumerGroupMember;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toSet;

public class ConsumerGroupsDescriber {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerGroupsDescriber.class);

    private final KafkaNamesMapper kafkaNamesMapper;
    private final AdminClient adminClient;
    private final LogEndOffsetChecker logEndOffsetChecker;

    public ConsumerGroupsDescriber(KafkaNamesMapper kafkaNamesMapper, AdminClient adminClient, LogEndOffsetChecker logEndOffsetChecker) {
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.adminClient = adminClient;
        this.logEndOffsetChecker = logEndOffsetChecker;
    }

    public Optional<ConsumerGroup> describeConsumerGroup(SubscriptionName subscription) {
        ConsumerGroupId consumerGroupId = kafkaNamesMapper.toConsumerGroupId(subscription);
        try {
            return describeConsumerGroup(consumerGroupId);
        } catch (Exception e) {
            logger.error("Failed to describe group with id: " + consumerGroupId.asString(), e);
            return Optional.empty();
        }
    }

    private Optional<ConsumerGroup> describeConsumerGroup(ConsumerGroupId consumerGroupId) throws ExecutionException, InterruptedException {
        Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsets = adminClient
                .listConsumerGroupOffsets(consumerGroupId.asString())
                .partitionsToOffsetAndMetadata()
                .get();
        Optional<ConsumerGroupDescription> description = adminClient
                .describeConsumerGroups(Collections.singletonList(consumerGroupId.asString()))
                .all()
                .get()
                .values()
                .stream()
                .findFirst();

        return description.map(d -> getKafkaConsumerGroup(topicPartitionOffsets, d));
    }

    private ConsumerGroup getKafkaConsumerGroup(Map<org.apache.kafka.common.TopicPartition, OffsetAndMetadata> topicPartitionOffsets, ConsumerGroupDescription description) {
        Set<ConsumerGroupMember> groupMembers = description.members().stream()
                .map(member -> getKafkaConsumerGroupMember(topicPartitionOffsets, member)).collect(toSet());

        return new ConsumerGroup(description.groupId(), description.state().toString(), groupMembers);
    }

    private ConsumerGroupMember getKafkaConsumerGroupMember(Map<org.apache.kafka.common.TopicPartition, OffsetAndMetadata> topicPartitionOffsets, MemberDescription member) {
        Set<pl.allegro.tech.hermes.api.TopicPartition> kafkaTopicPartitions = member.assignment().topicPartitions().stream().map(
                topicPartition -> {
                    Optional<OffsetAndMetadata> offset = Optional.ofNullable(topicPartitionOffsets.get(topicPartition));
                    return new pl.allegro.tech.hermes.api.TopicPartition(topicPartition.partition(),
                            topicPartition.topic(),
                            offset.map(OffsetAndMetadata::offset).orElse(0L),
                            logEndOffsetChecker.check(topicPartition),
                            offset.map(OffsetAndMetadata::metadata).orElse(""));
                }
        ).collect(toSet());
        return new ConsumerGroupMember(member.consumerId(), member.clientId(), member.host(), kafkaTopicPartitions);
    }
}
