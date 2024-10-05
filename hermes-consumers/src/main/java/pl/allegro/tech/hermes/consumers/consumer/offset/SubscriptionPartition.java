package pl.allegro.tech.hermes.consumers.consumer.offset;

import java.util.Objects;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;

public class SubscriptionPartition {

  private final KafkaTopicName kafkaTopicName;

  private final SubscriptionName subscriptionName;

  private final int partition;

  private final long partitionAssignmentTerm;

  public SubscriptionPartition(
      KafkaTopicName kafkaTopicName,
      SubscriptionName subscriptionName,
      int partition,
      long partitionAssignmentTerm) {
    this.kafkaTopicName = kafkaTopicName;
    this.subscriptionName = subscriptionName;
    this.partition = partition;
    this.partitionAssignmentTerm = partitionAssignmentTerm;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubscriptionPartition that = (SubscriptionPartition) o;
    return partition == that.partition
        && partitionAssignmentTerm == that.partitionAssignmentTerm
        && Objects.equals(kafkaTopicName, that.kafkaTopicName)
        && Objects.equals(subscriptionName, that.subscriptionName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kafkaTopicName, subscriptionName, partition, partitionAssignmentTerm);
  }

  @Override
  public String toString() {
    return "SubscriptionPartition{"
        + "kafkaTopicName="
        + kafkaTopicName
        + ", subscriptionName="
        + subscriptionName
        + ", partition="
        + partition
        + ", partitionAssignmentTerm="
        + partitionAssignmentTerm
        + '}';
  }
}
