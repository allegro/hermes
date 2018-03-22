package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

class KafkaConsumerOffsetMover implements PartitionAssigningAwareRetransmitter.OffsetMover {
    private KafkaConsumer consumer;

    KafkaConsumerOffsetMover(KafkaConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void move(SubscriptionPartitionOffset offset) {
        try {
            consumer.seek(new TopicPartition(offset.getKafkaTopicName().asString(), offset.getPartition()), offset.getOffset());
        } catch (IllegalStateException ex) {
            /*
                Unfortunately we can't differentiate between different kind of illegal states in KafkaConsumer.
                What we are really interested in is IllegalStateException with message containing
                "No current assignment for partition" but because this message can change in any minor release
                we can just do a leap of fate and interpret IllegalStateException as PartitionNotAssignedException.
                Throwing this exception should only cause retransmission to be retried after consumer rebalancing.
            */
            throw new PartitionNotAssignedException(ex);
        }
    }
}