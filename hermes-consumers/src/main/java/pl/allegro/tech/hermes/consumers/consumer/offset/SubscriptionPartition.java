package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;

import java.util.Objects;

public class SubscriptionPartition {

    private final KafkaTopicName kafkaTopicName;

    private final SubscriptionName subscriptionName;

    private final int partition;

    private final long partitionAssignmentTerm;

    private final long lastCommittedMessageTimestamp;

    public SubscriptionPartition(KafkaTopicName kafkaTopicName, SubscriptionName subscriptionName, int partition, long partitionAssignmentTerm, long lastCommittedMessageTimestamp) {
        this.kafkaTopicName = kafkaTopicName;
        this.subscriptionName = subscriptionName;
        this.partition = partition;
        this.partitionAssignmentTerm = partitionAssignmentTerm;
        this.lastCommittedMessageTimestamp = lastCommittedMessageTimestamp;
    }

    public KafkaTopicName getKafkaTopicName() {
        return kafkaTopicName;
    }

    public SubscriptionName getSubscriptionName() {
        return subscriptionName;
    }

    public int getPartition() {
        return partition;
    }

    public long getPartitionAssignmentTerm() {
        return partitionAssignmentTerm;
    }

    public long getLastCommittedMessageTimestamp() {
        return lastCommittedMessageTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionPartition that = (SubscriptionPartition) o;
        return partition == that.partition &&
                partitionAssignmentTerm == that.partitionAssignmentTerm &&
                Objects.equals(subscriptionName, that.subscriptionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionName, partition, partitionAssignmentTerm);
    }

    @Override
    public String toString() {
        return "SubscriptionPartition{" +
                "subscriptionName=" + subscriptionName +
                ", partition=" + partition +
                ", partitionAssignmentTerm=" + partitionAssignmentTerm +
                '}';
    }
}
