package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.consumers.consumer.offset.FailedToCommitOffsets;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsToCommit;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BrokerOffsetsRepository;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;

import javax.inject.Inject;

public class BrokerMessageCommitter implements MessageCommitter {

    private final BrokerOffsetsRepository offsetsRepository;

    @Inject
    public BrokerMessageCommitter(BrokerOffsetsRepository offsetsRepository) {
        this.offsetsRepository = offsetsRepository;
    }

    @Override
    public FailedToCommitOffsets commitOffsets(OffsetsToCommit offsetsToCommit) {
        return offsetsRepository.commit(offsetsToCommit);
    }

    @Override
    public void removeOffset(TopicName topicName, String subscriptionName, KafkaTopicName topic, int partition) throws Exception {
        //Consumers commit their offsets in Kafka by writing them to topic - so offsets will be removed after specified retention time.
    }
}
