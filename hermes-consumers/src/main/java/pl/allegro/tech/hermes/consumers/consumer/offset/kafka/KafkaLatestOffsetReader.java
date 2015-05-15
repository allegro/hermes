package pl.allegro.tech.hermes.consumers.consumer.offset.kafka;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetRequest;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPoolException;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static kafka.api.OffsetRequest.CurrentVersion;
import static kafka.api.OffsetRequest.LatestTime;

public class KafkaLatestOffsetReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaLatestOffsetReader.class);
    private static final int MAXIMUM_NUMBER_OF_OFFSETS = 1;

    private final BrokerStorage brokerStorage;
    private final PartitionOffsetRequestInfo partitionOffsetRequestInfo = new PartitionOffsetRequestInfo(
            LatestTime(),
            MAXIMUM_NUMBER_OF_OFFSETS);

    private final SimpleConsumerPool simpleConsumerPool;

    @Inject
    public KafkaLatestOffsetReader(
            BrokerStorage brokerStorage,
            SimpleConsumerPool simpleConsumerPool) {

        this.brokerStorage = brokerStorage;
        this.simpleConsumerPool = simpleConsumerPool;
    }

    public Map<TopicAndPartition, Long> read(Set<TopicAndPartition> topicAndPartitionSet) {
        Multimap<Integer, TopicAndPartition> leadersForPartitions = brokerStorage.readLeadersForPartitions(topicAndPartitionSet);
        Map<Integer, OffsetRequest> requestForLeaders = createRequestsForLeaders(leadersForPartitions);

        Map<TopicAndPartition, Long> latestOffsets = readLatestOffsetsFromLeaders(requestForLeaders, leadersForPartitions);

        return latestOffsets;
    }

    private Map<Integer, OffsetRequest> createRequestsForLeaders(Multimap<Integer, TopicAndPartition> leadersForPartitions)  {
        Map<Integer, OffsetRequest> offsetRequestsForLeaders = new HashMap<>(leadersForPartitions.size());



        for (Integer leaderId: leadersForPartitions.keySet()) {
            String clientId;
            try {
                clientId = simpleConsumerPool.get(leaderId).clientId();
                offsetRequestsForLeaders.put(leaderId, createOffsetRequestForLeader(clientId, leadersForPartitions.get(leaderId)));
            } catch (SimpleConsumerPoolException e) {
                LOGGER.warn("Error while getting simple consumer from pool for leader " + leaderId, e);
            }
        }
        return offsetRequestsForLeaders;
    }

    private OffsetRequest createOffsetRequestForLeader(String clientId, Collection<TopicAndPartition> topicAndPartitions) {
        Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<>(topicAndPartitions.size());
        for (TopicAndPartition topicAndPartition: topicAndPartitions) {
            requestInfo.put(topicAndPartition, partitionOffsetRequestInfo);
        }
        return new OffsetRequest(requestInfo, CurrentVersion(), clientId);
    }

    private Map<TopicAndPartition, Long> readLatestOffsetsFromLeaders(
            Map<Integer, OffsetRequest> offsetRequestForLeaders,
            Multimap<Integer, TopicAndPartition> leadersForPartitions) {

        Map<TopicAndPartition, Long> offsets = Maps.newHashMap();

        for (Map.Entry<Integer, OffsetRequest> entry: offsetRequestForLeaders.entrySet()) {
            offsets.putAll(readLatestOffsetsFromLeader(
                    entry.getKey(),
                    entry.getValue(),
                    leadersForPartitions.get(entry.getKey())));
        }

        return offsets;
    }

    private Map<TopicAndPartition, Long> readLatestOffsetsFromLeader(
            Integer leaderId,
            OffsetRequest offsetRequest,
            Collection<TopicAndPartition> topicAndPartitions) {

        try {
            SimpleConsumer simpleConsumer = simpleConsumerPool.get(leaderId);
            OffsetResponse response = simpleConsumer.getOffsetsBefore(offsetRequest);

            return readOffsetResponse(response, topicAndPartitions);
        } catch (Exception e) {
            LOGGER.warn("Something went wrong while reading data for broker with id " + leaderId, e);
            return Maps.newHashMap();
        }
    }

    private Map<TopicAndPartition, Long> readOffsetResponse(
            OffsetResponse offsetsResponse,
            Collection<TopicAndPartition> partitions) {

        Map<TopicAndPartition, Long> offsets = Maps.newHashMap();

        for (TopicAndPartition topicAndPartition: partitions) {
            offsets.put(
                    topicAndPartition,
                    offsetsResponse.offsets(topicAndPartition.topic(), topicAndPartition.partition())[0]
            );
        }

        return offsets;
    }


}