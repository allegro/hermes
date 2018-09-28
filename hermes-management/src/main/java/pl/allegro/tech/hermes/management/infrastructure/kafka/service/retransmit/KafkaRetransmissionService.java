package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import com.google.common.collect.Range;
 import java.util.Collections;
import java.util.Optional;
import kafka.common.TopicAndPartition;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaConsumerPool;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.management.domain.message.RetransmissionService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaRawMessageReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KafkaRetransmissionService implements RetransmissionService {

    private final BrokerStorage brokerStorage;
    private final KafkaRawMessageReader kafkaRawMessageReader;
    private final MessageContentWrapper messageContentWrapper;
    private final SubscriptionOffsetChangeIndicator subscriptionOffsetChange;
    private final KafkaConsumerPool consumerPool;
    private final KafkaNamesMapper kafkaNamesMapper;

    public KafkaRetransmissionService(
            BrokerStorage brokerStorage,
            KafkaRawMessageReader kafkaRawMessageReader,
            MessageContentWrapper messageContentWrapper,
            SubscriptionOffsetChangeIndicator subscriptionOffsetChange,
            KafkaConsumerPool consumerPool,
            KafkaNamesMapper kafkaNamesMapper) {

        this.brokerStorage = brokerStorage;
        this.kafkaRawMessageReader = kafkaRawMessageReader;
        this.messageContentWrapper = messageContentWrapper;
        this.subscriptionOffsetChange = subscriptionOffsetChange;
        this.consumerPool = consumerPool;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    public List<PartitionOffset> indicateOffsetChange(Topic topic, String subscription, String brokersClusterName,
                                                      long timestamp, boolean dryRun) {

        List<PartitionOffset> partitionOffsetList = new ArrayList<>();
        kafkaNamesMapper.toKafkaTopics(topic).forEach(k -> {
            List<Integer> partitionsIds = brokerStorage.readPartitionsIds(k.name().asString());

            for (Integer partitionId : partitionsIds) {
                KafkaConsumer<byte[], byte[]> consumer = createKafkaConsumer(k.name(), partitionId);
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

    private KafkaConsumer<byte[], byte[]> createKafkaConsumer(KafkaTopicName kafkaTopicName, int partition) {
        Integer leader = brokerStorage.readLeaderForPartition(new TopicAndPartition(kafkaTopicName.asString(), partition));

        return consumerPool.get(leader);
    }

    private long getLastOffset(KafkaConsumer<byte[], byte[]> consumer, Topic topic, KafkaTopic kafkaTopic, int partition, long timestamp) {
        Range<Long> offsetRange = getOffsetRange(consumer, kafkaTopic, partition);
        return search(topic, kafkaTopic, partition, offsetRange, timestamp);
    }

    private long search(Topic topic, KafkaTopic kafkaTopic, int partition, Range<Long> offsetRange, long timestamp) {
        OffsetSearcher searcher = new OffsetSearcher(
                new KafkaTimestampExtractor(topic, kafkaTopic, partition, kafkaRawMessageReader, messageContentWrapper)
        );
        return searcher.search(offsetRange, timestamp);
    }

    private Range<Long> getOffsetRange(KafkaConsumer<byte[], byte[]> kafkaConsumer, KafkaTopic kafkaTopic, int partition) {
        long earliestOffset = getBeginningOffset(kafkaConsumer, kafkaTopic, partition);
        long latestOffset = getEndingOffset(kafkaConsumer, kafkaTopic, partition);

        return Range.closed(earliestOffset, latestOffset);
    }

    private long getBeginningOffset(KafkaConsumer<byte[], byte[]> kafkaConsumer, KafkaTopic topicName, int partition) {
        TopicPartition topicPartition = new TopicPartition(topicName.name().asString(), partition);
        Map<TopicPartition, Long> offsets = kafkaConsumer.beginningOffsets(Collections.singleton(topicPartition));
        return Optional.ofNullable(offsets.get(topicPartition))
                .orElseThrow(() -> new OffsetNotFoundException(String.format("Beginning offset for partition %s not found", topicPartition)));
    }

    private long getEndingOffset(KafkaConsumer<byte[], byte[]> kafkaConsumer, KafkaTopic topicName, int partition) {
        TopicPartition topicPartition = new TopicPartition(topicName.name().asString(), partition);
        Map<TopicPartition, Long> offsets = kafkaConsumer.endOffsets(Collections.singleton(topicPartition));
        return Optional.ofNullable(offsets.get(topicPartition))
                .orElseThrow(() -> new OffsetNotFoundException(String.format("Ending offset for partition %s not found", topicPartition)));
    }
}
