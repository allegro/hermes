package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

public class KafkaConsumerOffsetMover {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerOffsetMover.class);
    private KafkaConsumer consumer;

    public KafkaConsumerOffsetMover(KafkaConsumer consumer) {
        this.consumer = consumer;
    }

    public boolean move(SubscriptionPartitionOffset offset) {
        try {
            TopicPartition tp = new TopicPartition(offset.getKafkaTopicName().asString(), offset.getPartition());
            if (consumer.assignment().contains(tp)) {
                logger.info("Moving offset for assigned partition {} on subscription {}",
                        offset.getPartition(), offset.getSubscriptionName());
                consumer.seek(tp, offset.getOffset());
                return true;
            } else {
                logger.info("Not assigned to partition {} on subscription {}",
                        offset.getPartition(), offset.getSubscriptionName());
                return false;
            }
        } catch (IllegalStateException ex) {
            logger.error("Failed to move offset for subscription={}, partition={}, offset={}",
                    offset.getSubscriptionName(), offset.getPartition(), offset.getOffset(), ex);
            return false;
        }
    }
}