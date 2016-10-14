package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import com.google.common.collect.Range;
import kafka.api.OffsetRequest;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.management.domain.message.RetransmissionService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaRawMessageReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KafkaRetransmissionService implements RetransmissionService {

    private final BrokerStorage brokerStorage;
    private final KafkaRawMessageReader kafkaRawMessageReader;
    private final MessageContentWrapper messageContentWrapper;
    private final SubscriptionOffsetChangeIndicator subscriptionOffsetChange;
    private final SimpleConsumerPool simpleConsumerPool;
    private final KafkaNamesMapper kafkaNamesMapper;
    private final SchemaRepository schemaRepository;

    public KafkaRetransmissionService(
            BrokerStorage brokerStorage,
            KafkaRawMessageReader kafkaRawMessageReader,
            MessageContentWrapper messageContentWrapper,
            SubscriptionOffsetChangeIndicator subscriptionOffsetChange,
            SimpleConsumerPool simpleConsumerPool,
            KafkaNamesMapper kafkaNamesMapper,
            SchemaRepository schemaRepository) {

        this.brokerStorage = brokerStorage;
        this.kafkaRawMessageReader = kafkaRawMessageReader;
        this.messageContentWrapper = messageContentWrapper;
        this.subscriptionOffsetChange = subscriptionOffsetChange;
        this.simpleConsumerPool = simpleConsumerPool;
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.schemaRepository = schemaRepository;
    }

    @Override
    public List<PartitionOffset> indicateOffsetChange(Topic topic, String subscription, String brokersClusterName,
                                                      long timestamp, boolean dryRun) {

        List<PartitionOffset> partitionOffsetList = new ArrayList<>();
        kafkaNamesMapper.toKafkaTopics(topic).forEach(k -> {
            List<Integer> partitionsIds = brokerStorage.readPartitionsIds(k.name().asString());

            for (Integer partitionId : partitionsIds) {
                SimpleConsumer consumer = createSimpleConsumer(k.name(), partitionId);
                long offset = getLastOffset(consumer, topic, k, partitionId, timestamp);
                PartitionOffset partitionOffset = new PartitionOffset(k.name(), offset, partitionId);
                partitionOffsetList.add(partitionOffset);
                if (!dryRun) {
                    subscriptionOffsetChange.setSubscriptionOffset(topic.getName(), subscription, brokersClusterName, partitionOffset);
                }
            }
        });

        return partitionOffsetList;
    }

    private SimpleConsumer createSimpleConsumer(KafkaTopicName kafkaTopicName, int partition) {
        Integer leader = brokerStorage.readLeaderForPartition(new TopicAndPartition(kafkaTopicName.asString(), partition));

        return simpleConsumerPool.get(leader);
    }

    private long getLastOffset(SimpleConsumer consumer, Topic topic, KafkaTopic kafkaTopic, int partition, long timestamp) {
        Range<Long> offsetRange = getOffsetRange(consumer, kafkaTopic, partition);
        return search(topic, kafkaTopic, partition, offsetRange, timestamp);
    }

    private long search(Topic topic, KafkaTopic kafkaTopic, int partition, Range<Long> offsetRange, long timestamp) {
        OffsetSearcher searcher = new OffsetSearcher(
                new KafkaTimestampExtractor(topic, kafkaTopic, partition, kafkaRawMessageReader, messageContentWrapper, schemaRepository)
        );
        return searcher.search(offsetRange, timestamp);
    }

    private Range<Long> getOffsetRange(SimpleConsumer simpleConsumer, KafkaTopic kafkaTopic, int partition) {
        long earliestOffset = getOffset(simpleConsumer, kafkaTopic, partition, OffsetRequest.EarliestTime());
        long latestOffset = getOffset(simpleConsumer, kafkaTopic, partition, OffsetRequest.LatestTime());

        return Range.closed(earliestOffset, latestOffset);
    }

    private long getOffset(SimpleConsumer simpleConsumer, KafkaTopic topicName, int partition, long whichTime) {
        TopicAndPartition topicAndPartition = new TopicAndPartition(topicName.name().asString(), partition);

        Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<>();
        requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(whichTime, 1));

        kafka.javaapi.OffsetRequest request =
                new kafka.javaapi.OffsetRequest(requestInfo, OffsetRequest.CurrentVersion(), "KafkaRetransmissionService" + topicName.name().asString());
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
