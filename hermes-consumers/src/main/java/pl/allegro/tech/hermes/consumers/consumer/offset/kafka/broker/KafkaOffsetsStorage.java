package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartition;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

import javax.inject.Inject;

public class KafkaOffsetsStorage implements OffsetsStorage {

    private final BrokerOffsetsRepository brokerOffsetsRepository;

    @Inject
    public KafkaOffsetsStorage(BrokerOffsetsRepository brokerOffsetsRepository) {
        this.brokerOffsetsRepository = brokerOffsetsRepository;
    }

    @Override
    public void moveSubscriptionOffset(SubscriptionPartitionOffset subscriptionPartitionOffset) throws Exception {
        brokerOffsetsRepository.moveOffset(subscriptionPartitionOffset);
    }

    @Override
    public long getSubscriptionOffset(SubscriptionPartition subscriptionPartition) {
        return brokerOffsetsRepository.findOffset(subscriptionPartition);
    }
}
