package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.exception.BrokerNotFoundForPartitionException;
import pl.allegro.tech.hermes.common.exception.PartitionsNotFoundForGivenTopicException;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;

import java.util.HashMap;
import java.util.Map;

public class OffsetsAvailableChecker {

    private static final Logger logger = LoggerFactory.getLogger(OffsetsAvailableChecker.class);

    private final SimpleConsumerPool simpleConsumerPool;
    private final BrokerStorage storage;

    public OffsetsAvailableChecker(SimpleConsumerPool simpleConsumerPool, BrokerStorage storage) {
        this.simpleConsumerPool = simpleConsumerPool;
        this.storage = storage;
    }

    public boolean areOffsetsAvailable(KafkaTopic topic) {
        try {
            return storage.readPartitionsIds(topic.name().asString()).stream().allMatch(partition -> {
                TopicAndPartition topicAndPartition = new TopicAndPartition(topic.name().asString(), partition);

                Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<>();
                requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(0, 1));

                kafka.javaapi.OffsetRequest request =
                        new kafka.javaapi.OffsetRequest(requestInfo, kafka.api.OffsetRequest.CurrentVersion(), "OffsetsAvailableChecker_" + topic.name().asString());
                return !simpleConsumerPool.get(topic, partition).getOffsetsBefore(request).hasError();
            });
        } catch (PartitionsNotFoundForGivenTopicException | BrokerNotFoundForPartitionException e) {
            logger.debug("Offsets reported as not available due to failure", e);
            return false;
        }
    }
}
