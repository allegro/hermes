package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import java.util.Collections;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.exception.BrokerNotFoundForPartitionException;
import pl.allegro.tech.hermes.common.exception.PartitionsNotFoundForGivenTopicException;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaConsumerPool;

public class OffsetsAvailableChecker {

    private static final Logger logger = LoggerFactory.getLogger(OffsetsAvailableChecker.class);

    private final KafkaConsumerPool consumerPool;
    private final BrokerStorage storage;

    public OffsetsAvailableChecker(KafkaConsumerPool consumerPool, BrokerStorage storage) {
        this.consumerPool = consumerPool;
        this.storage = storage;
    }

    public boolean areOffsetsAvailable(KafkaTopic topic) {
        try {
            return storage.readPartitionsIds(topic.name().asString()).stream().allMatch(partition -> {
                TopicPartition topicPartition = new TopicPartition(topic.name().asString(), partition);
                consumerPool.get(topic, partition).beginningOffsets(Collections.singleton(topicPartition));
                return true;
            });
        } catch (PartitionsNotFoundForGivenTopicException | BrokerNotFoundForPartitionException |
                org.apache.kafka.common.errors.TimeoutException e) {
            logger.debug("Offsets reported as not available due to failure", e);
            return false;
        }
    }
}
