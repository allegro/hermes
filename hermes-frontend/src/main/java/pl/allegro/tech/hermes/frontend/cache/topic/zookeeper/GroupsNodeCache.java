package pl.allegro.tech.hermes.frontend.cache.topic.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.cache.zookeeper.NodeCache;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicCallback;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.util.Optional;
import java.util.concurrent.ExecutorService;


class GroupsNodeCache extends NodeCache<TopicCallback, TopicsNodeCache> {

    private final HermesMetrics hermesMetrics;
    private final KafkaNamesMapper kafkaNamesMapper;

    GroupsNodeCache(CuratorFramework curatorClient, ObjectMapper objectMapper, String path,
                    ExecutorService executorService, HermesMetrics hermesMetrics, KafkaNamesMapper kafkaNamesMapper) {
        super(curatorClient, objectMapper, path, executorService);
        this.hermesMetrics = hermesMetrics;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    protected TopicsNodeCache createSubcache(String path) {
        return new TopicsNodeCache(
                curatorClient, objectMapper, topicsNodePath(path), executorService, hermesMetrics, kafkaNamesMapper);
    }

    private String topicsNodePath(String path) {
        return path + "/" + ZookeeperPaths.TOPICS_PATH;
    }

    public Optional<CachedTopic> getTopic(TopicName topicName) {
        TopicsNodeCache topicsNodeCache = getEntry(topicName.getGroupName());
        return topicsNodeCache != null ? Optional.ofNullable(topicsNodeCache.getTopic(topicName.getName())) : Optional.empty();
    }
}
