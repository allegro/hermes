package pl.allegro.tech.hermes.consumers.subscription.cache.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.cache.zookeeper.NodeCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionCallback;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

class GroupsNodeCache extends NodeCache<SubscriptionCallback, TopicsNodeCache> {
    private final ExecutorService processingExecutor;

    public GroupsNodeCache(CuratorFramework curatorClient, ObjectMapper objectMapper, String path, ExecutorService eventExecutor, ExecutorService processingExecutor) {
        super(curatorClient, objectMapper, path, eventExecutor);
        this.processingExecutor = processingExecutor;
    }

    @Override
    protected TopicsNodeCache createSubcache(String path) {
        return new TopicsNodeCache(curatorClient, objectMapper, topicsNodePath(path), executorService, processingExecutor);
    }

    private String topicsNodePath(String path) {
        return path + "/" + ZookeeperPaths.TOPICS_PATH;
    }

    public List<SubscriptionName> listActiveSubscriptionNames() {
        return getSubcacheEntrySet().stream()
                .map(entry -> entry.getValue().listActiveSubscriptionNames())
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
