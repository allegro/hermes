package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class KafkaPartitionLeaderRegistry {

    private final AdminClient adminClient;
    private final TopicsCache topicsCache;
    private volatile Map<TopicPartition, String> leaders = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(KafkaPartitionLeaderRegistry.class);

    public KafkaPartitionLeaderRegistry(TopicsCache topicsCache, AdminClient adminClient) {
        this.topicsCache = topicsCache;
        this.adminClient = adminClient;
    }

    public Optional<String> leaderOf(String topic, int partition) {
        String maybeLeader = leaders.get(new TopicPartition(topic, partition));
        return Optional.ofNullable(maybeLeader);
    }

    private static Map.Entry<TopicPartition, String> registryEntry(TopicDescription topic, TopicPartitionInfo partition) {
        return Map.entry(new TopicPartition(topic.name(), partition.partition()), partition.leader().host());
    }

    private List<String> getTopicNames() {
        return topicsCache.getTopics().stream().map(
                CachedTopic::getQualifiedName
        ).collect(Collectors.toList());
    }

    public void updateLeaders() {
        List<String> topicNames = getTopicNames();
        DescribeTopicsResult result = adminClient.describeTopics(topicNames);
        try {
            Collection<TopicDescription> topics = result.all().get().values();
            this.leaders = topics.stream()
                    .flatMap(topic -> topic.partitions().stream()
                            .map(partition -> registryEntry(topic, partition))
                    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            logger.warn("Failed to update leader registry. Broker latency metrics may be incorrect", e);
        }
    }
}
