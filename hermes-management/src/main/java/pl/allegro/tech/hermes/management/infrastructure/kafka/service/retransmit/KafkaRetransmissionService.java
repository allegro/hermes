package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.kafka.KafkaConsumerPool;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.management.domain.message.RetransmissionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KafkaRetransmissionService implements RetransmissionService {

    private final BrokerStorage brokerStorage;
    private final SubscriptionOffsetChangeIndicator subscriptionOffsetChange;
    private final KafkaConsumerPool consumerPool;
    private final KafkaNamesMapper kafkaNamesMapper;

    public KafkaRetransmissionService(
            BrokerStorage brokerStorage,
            SubscriptionOffsetChangeIndicator subscriptionOffsetChange,
            KafkaConsumerPool consumerPool,
            KafkaNamesMapper kafkaNamesMapper) {

        this.brokerStorage = brokerStorage;
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
                KafkaConsumer<byte[], byte[]> consumer = createKafkaConsumer(k, partitionId);
                long offset = findClosestOffsetJustBeforeTimestamp(consumer, k, partitionId, timestamp);
                PartitionOffset partitionOffset = new PartitionOffset(k.name(), offset, partitionId);
                partitionOffsetList.add(partitionOffset);
                if (!dryRun) {
                    subscriptionOffsetChange.setSubscriptionOffset(topic.getName(), subscription, brokersClusterName, partitionOffset);
                }
            }
        });

        return partitionOffsetList;
    }

    @Override
    public boolean areOffsetsMoved(Topic topic, String subscriptionName, String brokersClusterName) {
        return kafkaNamesMapper.toKafkaTopics(topic).allMatch(kafkaTopic -> {
            List<Integer> partitionIds = brokerStorage.readPartitionsIds(kafkaTopic.name().asString());
            return subscriptionOffsetChange.areOffsetsMoved(topic.getName(), subscriptionName, brokersClusterName, kafkaTopic, partitionIds);
        });
    }

    private KafkaConsumer<byte[], byte[]> createKafkaConsumer(KafkaTopic kafkaTopic, int partition) {
        return consumerPool.get(kafkaTopic, partition);
    }

    private long findClosestOffsetJustBeforeTimestamp(KafkaConsumer<byte[], byte[]> consumer, KafkaTopic kafkaTopic, int partition, long timestamp) {
        long endOffset = getEndingOffset(consumer, kafkaTopic, partition);
        TopicPartition topicPartition = new TopicPartition(kafkaTopic.name().asString(), partition);
        return Optional.ofNullable(consumer.offsetsForTimes(Collections.singletonMap(topicPartition, timestamp)).get(topicPartition))
                .orElse(new OffsetAndTimestamp(endOffset, timestamp)).offset();
    }

    private long getEndingOffset(KafkaConsumer<byte[], byte[]> kafkaConsumer, KafkaTopic topicName, int partition) {
        TopicPartition topicPartition = new TopicPartition(topicName.name().asString(), partition);
        Map<TopicPartition, Long> offsets = kafkaConsumer.endOffsets(Collections.singleton(topicPartition));
        return Optional.ofNullable(offsets.get(topicPartition))
                .orElseThrow(() -> new OffsetNotFoundException(String.format("Ending offset for partition %s not found", topicPartition)));
    }
}
