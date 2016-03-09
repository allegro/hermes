package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

public interface OffsetsStorage {

    void setSubscriptionOffset(SubscriptionName subscription, PartitionOffset partitionOffset) throws Exception;

    long getSubscriptionOffset(SubscriptionName subscription, KafkaTopicName kafkaTopicName, int partitionId);
}
