package pl.allegro.tech.hermes.consumers.consumer.offset;

import java.util.Objects;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

public class SubscriptionPartitionOffset {

  private final SubscriptionPartition subscriptionPartition;

  private final long offset;

  public SubscriptionPartitionOffset(SubscriptionPartition subscriptionPartition, long offset) {
    this.subscriptionPartition = subscriptionPartition;
    this.offset = offset;
  }

  public static SubscriptionPartitionOffset subscriptionPartitionOffset(
      SubscriptionName subscriptionName,
      PartitionOffset partitionOffset,
      long partitionAssignmentTerm) {
    return new SubscriptionPartitionOffset(
        new SubscriptionPartition(
            partitionOffset.getTopic(),
            subscriptionName,
            partitionOffset.getPartition(),
            partitionAssignmentTerm),
        partitionOffset.getOffset());
  }

  public SubscriptionName getSubscriptionName() {
    return subscriptionPartition.getSubscriptionName();
  }

  public KafkaTopicName getKafkaTopicName() {
    return subscriptionPartition.getKafkaTopicName();
  }

  public int getPartition() {
    return subscriptionPartition.getPartition();
  }

  public long getOffset() {
    return offset;
  }

  public PartitionOffset getPartitionOffset() {
    return new PartitionOffset(
        subscriptionPartition.getKafkaTopicName(), offset, subscriptionPartition.getPartition());
  }

  public long getPartitionAssignmentTerm() {
    return subscriptionPartition.getPartitionAssignmentTerm();
  }

  public SubscriptionPartition getSubscriptionPartition() {
    return subscriptionPartition;
  }

  @Override
  public String toString() {
    return "SubscriptionPartitionOffset{"
        + "subscriptionPartition="
        + subscriptionPartition
        + ", offset="
        + offset
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubscriptionPartitionOffset that = (SubscriptionPartitionOffset) o;
    return offset == that.offset
        && Objects.equals(subscriptionPartition, that.subscriptionPartition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subscriptionPartition, offset);
  }
}
