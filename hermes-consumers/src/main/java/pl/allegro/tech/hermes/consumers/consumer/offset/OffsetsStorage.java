package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

public interface OffsetsStorage {

    void moveSubscriptionOffset(SubscriptionPartitionOffset subscriptionPartitionOffset) throws Exception;

    long getSubscriptionOffset(SubscriptionPartition subscriptionPartition);
}
