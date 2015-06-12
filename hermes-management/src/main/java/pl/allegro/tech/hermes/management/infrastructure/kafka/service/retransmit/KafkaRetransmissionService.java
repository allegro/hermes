package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;


import com.google.common.collect.Range;
import kafka.api.OffsetRequest;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;
import pl.allegro.tech.hermes.domain.subscription.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.management.domain.message.RetransmissionService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaSingleMessageReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KafkaRetransmissionService implements RetransmissionService {

    private final BrokerStorage brokerStorage;
    private final KafkaSingleMessageReader kafkaSingleMessageReader;
    private final JsonMessageContentWrapper messageContentWrapper;
    private final SubscriptionOffsetChangeIndicator subscriptionOffsetChange;
    private final SimpleConsumerPool simpleConsumerPool;

    public KafkaRetransmissionService(
            BrokerStorage brokerStorage,
            KafkaSingleMessageReader kafkaSingleMessageReader,
            JsonMessageContentWrapper messageContentWrapper,
            SubscriptionOffsetChangeIndicator subscriptionOffsetChange,
            SimpleConsumerPool simpleConsumerPool) {

        this.brokerStorage = brokerStorage;
        this.kafkaSingleMessageReader = kafkaSingleMessageReader;
        this.messageContentWrapper = messageContentWrapper;
        this.subscriptionOffsetChange = subscriptionOffsetChange;
        this.simpleConsumerPool = simpleConsumerPool;
    }

    @Override
    public void indicateOffsetChange(TopicName topic, String subscription, String brokersClusterName, long timestamp) {
        List<Integer> partitionsIds = brokerStorage.readPartitionsIds(topic.qualifiedName());

        for (Integer partitionId : partitionsIds) {
            SimpleConsumer consumer = createSimpleConsumer(topic, partitionId);
            long offset = getLastOffset(consumer, topic, partitionId, timestamp);
            subscriptionOffsetChange.setSubscriptionOffset(topic, subscription, brokersClusterName, partitionId, offset);
        }
    }

    private SimpleConsumer createSimpleConsumer(TopicName topic, int partition) {
        Integer leader = brokerStorage.readLeaderForPartition(new TopicAndPartition(topic.qualifiedName(), partition));

        return simpleConsumerPool.get(leader);
    }

    private long getLastOffset(SimpleConsumer consumer, TopicName topic, int partition, long timestamp) {
        Range<Long> offsetRange = getOffsetRange(consumer, topic, partition);
        return search(topic, partition, offsetRange, timestamp);
    }

    private long search(TopicName topic, int partition, Range<Long> offsetRange, long timestamp) {
        OffsetSearcher searcher = new OffsetSearcher(
                new KafkaTimestampExtractor(topic, partition, kafkaSingleMessageReader, messageContentWrapper)
        );
        return searcher.search(offsetRange, timestamp);
    }

    private Range<Long> getOffsetRange(SimpleConsumer simpleConsumer, TopicName topic, int partition) {
        long earliestOffset = getOffset(simpleConsumer, topic, partition, OffsetRequest.EarliestTime());
        long latestOffset = getOffset(simpleConsumer, topic, partition, OffsetRequest.LatestTime());

        return Range.closed(earliestOffset, latestOffset);
    }

    private long getOffset(SimpleConsumer simpleConsumer, TopicName topic, int partition, long whichTime) {
        TopicAndPartition topicAndPartition = new TopicAndPartition(topic.qualifiedName(), partition);

        Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<>();
        requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(whichTime, 1));

        kafka.javaapi.OffsetRequest request =
                new kafka.javaapi.OffsetRequest(requestInfo, OffsetRequest.CurrentVersion(), "KafkaRetransmissionService" + topic);
        OffsetResponse response = simpleConsumer.getOffsetsBefore(request);

        return readOffsetFromResponse(response, topicAndPartition);
    }

    private long readOffsetFromResponse(OffsetResponse response, TopicAndPartition topicAndPartition) {
        if (response.hasError()) {
            throw new OffsetNotFoundException(response.errorCode(topicAndPartition.topic(), topicAndPartition.partition()));
        }

        long[] offsets = response.offsets(topicAndPartition.topic(), topicAndPartition.partition());
        return offsets[0];
    }
}
