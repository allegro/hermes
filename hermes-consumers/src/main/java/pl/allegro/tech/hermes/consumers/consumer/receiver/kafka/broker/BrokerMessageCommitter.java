package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BrokerOffsetsRepository;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import javax.inject.Inject;

public class BrokerMessageCommitter implements MessageCommitter {

    private final BrokerOffsetsRepository offsetsRepository;

    @Inject
    public BrokerMessageCommitter(BrokerOffsetsRepository offsetsRepository) {
        this.offsetsRepository = offsetsRepository;
    }

    @Override
    public void commitOffset(Subscription subscription, PartitionOffset partitionOffset) throws Exception {
        offsetsRepository.save(subscription, new PartitionOffset(partitionOffset.getOffset() + 1, partitionOffset.getPartition()));
    }

    @Override
    public void removeOffset(TopicName topicName, String subscriptionName, int partition) throws Exception {
        //Consumers commit their offsets in Kafka by writing them to topic - so offsets will be removed after specified retention time.
    }
}
