package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;

import java.util.Objects;

public class SubscriptionPartition {

    private final KafkaTopicName kafkaTopicName;

    private final SubscriptionName subscription;

    private final int partition;

    public SubscriptionPartition(KafkaTopicName kafkaTopicName, SubscriptionName subscription, int partition) {
        this.kafkaTopicName = kafkaTopicName;
        this.subscription = subscription;
        this.partition = partition;
    }

    public static SubscriptionPartition subscriptionPartition(String kafkaTopicName, String subscriptionName, int partition) {
        return new SubscriptionPartition(
                KafkaTopicName.valueOf(kafkaTopicName),
                SubscriptionName.fromString(subscriptionName),
                partition
        );
    }

    public KafkaTopicName getKafkaTopicName() {
        return kafkaTopicName;
    }

    public SubscriptionName getSubscriptionName() {
        return subscription;
    }

    public int getPartition() {
        return partition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionPartition that = (SubscriptionPartition) o;
        return partition == that.partition &&
                Objects.equals(subscription, that.subscription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscription, partition);
    }

    @Override
    public String toString() {
        return "SubscriptionPartition{" +
                "subscription=" + subscription +
                ", partition=" + partition +
                '}';
    }
}
