package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

public interface OffsetsStorage {

    void setSubscriptionOffset(Subscription subscription, PartitionOffset partitionOffset) throws Exception;

    long getSubscriptionOffset(Subscription subscription, KafkaTopic kafkaTopic, int partitionId);
}
