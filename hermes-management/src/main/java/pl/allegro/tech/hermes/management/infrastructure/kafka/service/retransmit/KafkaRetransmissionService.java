package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;


import com.google.common.collect.Range;
import kafka.api.OffsetRequest;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;
import pl.allegro.tech.hermes.domain.subscription.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.message.RetransmissionService;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KafkaRetransmissionService implements RetransmissionService {

    private final BrokerStorage brokerStorage;
    private final SingleMessageReader singleMessageReader;
    private final MessageContentWrapper messageContentWrapper;
    private final SubscriptionOffsetChangeIndicator subscriptionOffsetChange;
    private final SimpleConsumerPool simpleConsumerPool;
    private final TopicRepository topicRepository;

    public KafkaRetransmissionService(
            BrokerStorage brokerStorage,
            SingleMessageReader singleMessageReader,
            MessageContentWrapper messageContentWrapper,
            SubscriptionOffsetChangeIndicator subscriptionOffsetChange,
            SimpleConsumerPool simpleConsumerPool,
            TopicRepository topicRepository) {

        this.brokerStorage = brokerStorage;
        this.singleMessageReader = singleMessageReader;
        this.messageContentWrapper = messageContentWrapper;
        this.subscriptionOffsetChange = subscriptionOffsetChange;
        this.simpleConsumerPool = simpleConsumerPool;
        this.topicRepository = topicRepository;
    }

    @Override
    public List<PartitionOffset> indicateOffsetChange(TopicName topic, String subscription, String brokersClusterName,
                                                            long timestamp, boolean dryRun) {

        List<PartitionOffset> partitionOffsetList = new ArrayList<>();
        List<Integer> partitionsIds = brokerStorage.readPartitionsIds(topic.qualifiedName());

        for (Integer partitionId : partitionsIds) {
            SimpleConsumer consumer = createSimpleConsumer(topic, partitionId);
            long offset = getLastOffset(consumer, topicRepository.getTopicDetails(topic), partitionId, timestamp);
            partitionOffsetList.add(new PartitionOffset(offset, partitionId));
            if (!dryRun) {
                subscriptionOffsetChange.setSubscriptionOffset(topic, subscription, brokersClusterName, partitionId, offset);
            }
        }

        return partitionOffsetList;
    }

    private SimpleConsumer createSimpleConsumer(TopicName topic, int partition) {
        Integer leader = brokerStorage.readLeaderForPartition(new TopicAndPartition(topic.qualifiedName(), partition));

        return simpleConsumerPool.get(leader);
    }

    private long getLastOffset(SimpleConsumer consumer, Topic topic, int partition, long timestamp) {
        Range<Long> offsetRange = getOffsetRange(consumer, topic, partition);
        return search(topic, partition, offsetRange, timestamp);
    }

    private long search(Topic topic, int partition, Range<Long> offsetRange, long timestamp) {
        OffsetSearcher searcher = new OffsetSearcher(
                new KafkaTimestampExtractor(topic, partition, singleMessageReader, messageContentWrapper)
        );
        return searcher.search(offsetRange, timestamp);
    }

    private Range<Long> getOffsetRange(SimpleConsumer simpleConsumer, Topic topic, int partition) {
        long earliestOffset = getOffset(simpleConsumer, topic, partition, OffsetRequest.EarliestTime());
        long latestOffset = getOffset(simpleConsumer, topic, partition, OffsetRequest.LatestTime());

        return Range.closed(earliestOffset, latestOffset);
    }

    private long getOffset(SimpleConsumer simpleConsumer, Topic topic, int partition, long whichTime) {
        TopicAndPartition topicAndPartition = new TopicAndPartition(topic.getQualifiedName(), partition);

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
