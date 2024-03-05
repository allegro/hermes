package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerTopicMetadataFetcher;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.kafka.common.config.ConfigResource.Type.TOPIC;
import static org.apache.kafka.common.config.TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG;

public class KafkaBrokerTopicMetadataFetcher implements BrokerTopicMetadataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaBrokerTopicMetadataFetcher.class);

    private final Producers producers;
    private final LoadingCache<String, Integer> minInSyncReplicasCache;
    private final AdminClient adminClient;

    public KafkaBrokerTopicMetadataFetcher(Producers producers, AdminClient adminClient, Duration metadataMaxAge) {
        this.producers = producers;
        this.adminClient = adminClient;
        this.minInSyncReplicasCache = CacheBuilder
                .newBuilder()
                .expireAfterWrite(metadataMaxAge.toMillis(), MILLISECONDS)
                .build(new MinInSyncReplicasLoader());
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
        int minInSyncReplicas = minInSyncReplicasCache.get(kafkaTopicName);
        return partitionInfos.stream().anyMatch(p -> p.inSyncReplicas().length < minInSyncReplicas);
    }

    @Override
    public void close() {
        adminClient.close();
    }

    private class MinInSyncReplicasLoader extends CacheLoader<String, Integer> {

        @Override
        public Integer load(String kafkaTopicName) throws Exception {
            ConfigResource resource = new ConfigResource(TOPIC, kafkaTopicName);
            DescribeConfigsResult describeTopicsResult = adminClient.describeConfigs(ImmutableList.of(resource));
            Map<ConfigResource, Config> configMap = describeTopicsResult.all().get();
            Config config = configMap.get(resource);
            ConfigEntry configEntry = config.get(MIN_IN_SYNC_REPLICAS_CONFIG);
            String value = configEntry.value();
            return Integer.parseInt(value);
        }
    }
}
