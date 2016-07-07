package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.Objects;

public class SubscriptionPartitionOffset {

    private final SubscriptionPartition subscriptionPartition;

    private final long offset;

    public SubscriptionPartitionOffset(SubscriptionPartition subscriptionPartition, long offset) {
        this.subscriptionPartition = subscriptionPartition;
        this.offset = offset;
    }

    public static SubscriptionPartitionOffset subscriptionPartitionOffset(String kafkaTopicName, String subscriptionName, int partition, long offset) {
        return new SubscriptionPartitionOffset(
                SubscriptionPartition.subscriptionPartition(kafkaTopicName, subscriptionName, partition),
                offset
        );
    }

    public static SubscriptionPartitionOffset subscriptionPartitionOffset(Message message, Subscription subscription) {
        return subscriptionPartitionOffset(message.getPartitionOffset(), subscription);
    }

    public static SubscriptionPartitionOffset subscriptionPartitionOffset(PartitionOffset partitionOffset, Subscription subscription) {
        return new SubscriptionPartitionOffset(
                new SubscriptionPartition(
                        partitionOffset.getTopic(),
                        subscription.getQualifiedName(),
                        partitionOffset.getPartition()
                ),
                partitionOffset.getOffset()
        );
    }

    public static SubscriptionPartitionOffset subscriptionPartitionOffset(PartitionOffset partitionOffset, SubscriptionName subscription) {
        return new SubscriptionPartitionOffset(
                new SubscriptionPartition(
                        partitionOffset.getTopic(),
                        subscription,
                        partitionOffset.getPartition()
                ),
                partitionOffset.getOffset()
        );
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

    public SubscriptionPartition getSubscriptionPartition() {
        return subscriptionPartition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionPartitionOffset that = (SubscriptionPartitionOffset) o;
        return offset == that.offset &&
                Objects.equals(subscriptionPartition, that.subscriptionPartition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionPartition, offset);
    }

    @Override
    public String toString() {
        return "SubscriptionPartitionOffset{" +
                "subscriptionPartition=" + subscriptionPartition +
                ", offset=" + offset +
                '}';
    }
}
