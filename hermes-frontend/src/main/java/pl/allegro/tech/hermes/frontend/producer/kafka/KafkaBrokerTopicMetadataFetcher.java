package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerTopicMetadataFetcher;

import java.util.List;

public class KafkaBrokerTopicMetadataFetcher implements BrokerTopicMetadataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaBrokerTopicMetadataFetcher.class);

    private final Producers producers;
    private final KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher;

    public KafkaBrokerTopicMetadataFetcher(Producers producers, KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher) {
        this.producers = producers;
        this.kafkaTopicMetadataFetcher = kafkaTopicMetadataFetcher;
    }

    @Override
    public boolean tryFetchFromLocalDatacenter(CachedTopic cachedTopic) {
        String kafkaTopicName = cachedTopic.getKafkaTopics().getPrimary().name().asString();

        try {
            List<PartitionInfo> partitionInfos = producers.get(cachedTopic.getTopic()).partitionsFor(kafkaTopicName);
            if (anyPartitionWithoutLeader(partitionInfos)) {
                logger.warn("Topic {} has partitions without a leader.", kafkaTopicName);
                return false;
            }
            if (anyUnderReplicatedPartition(partitionInfos, kafkaTopicName)) {
                logger.warn("Topic {} has under replicated partitions.", kafkaTopicName);
                return false;
            }
            if (partitionInfos.size() > 0) {
                return true;
            }
        } catch (Exception e) {
            logger.warn("Could not read information about partitions for topic {}. {}", kafkaTopicName, e.getMessage());
            return false;
        }

        logger.warn("No information about partitions for topic {}", kafkaTopicName);
        return false;
    }

    @Override
    public boolean tryFetchFromDatacenter(CachedTopic topic, String datacenter) {
        // TODO: To fetch metadata for a selected datacenter https://github.com/allegro/hermes/pull/1823 is required.
        return tryFetchFromLocalDatacenter(topic);
    }

    private boolean anyPartitionWithoutLeader(List<PartitionInfo> partitionInfos) {
        return partitionInfos.stream().anyMatch(p -> p.leader() == null);
    }

    private boolean anyUnderReplicatedPartition(List<PartitionInfo> partitionInfos, String kafkaTopicName) throws Exception {
        int minInSyncReplicas = kafkaTopicMetadataFetcher.fetchMinInSyncReplicas(kafkaTopicName);
        return partitionInfos.stream().anyMatch(p -> p.inSyncReplicas().length < minInSyncReplicas);
    }
}
