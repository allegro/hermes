package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.common.config.ConfigResource;

import java.time.Duration;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.kafka.common.config.ConfigResource.Type.TOPIC;
import static org.apache.kafka.common.config.TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG;

public class KafkaTopicMetadataFetcher {
    private final LoadingCache<String, Integer> minInSyncReplicasCache;
    private final AdminClient adminClient;

    KafkaTopicMetadataFetcher(AdminClient adminClient, Duration metadataMaxAge) {
        this.adminClient = adminClient;
        this.minInSyncReplicasCache = CacheBuilder
                .newBuilder()
                .expireAfterWrite(metadataMaxAge.toMillis(), MILLISECONDS)
                .build(new MinInSyncReplicasLoader());
    }

    int fetchMinInSyncReplicas(String kafkaTopicName) throws Exception {
        return minInSyncReplicasCache.get(kafkaTopicName);
    }

    void close() {
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
