package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

import javax.inject.Inject;

public class KafkaOffsetsStorage implements OffsetsStorage {

    private final BrokerOffsetsRepository brokerOffsetsRepository;

    @Inject
    public KafkaOffsetsStorage(BrokerOffsetsRepository brokerOffsetsRepository) {
        this.brokerOffsetsRepository = brokerOffsetsRepository;
    }

    @Override
    public void setSubscriptionOffset(Subscription subscription, PartitionOffset partitionOffset) throws Exception {
        brokerOffsetsRepository.saveIfOffsetInThePast(subscription, partitionOffset);
    }

    @Override
    public long getSubscriptionOffset(Subscription subscription, KafkaTopicName kafkaTopicName, int partitionId) {
        return brokerOffsetsRepository.find(subscription, kafkaTopicName, partitionId);
    }
}
