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

class TopicsNodeCache extends NodeCache<SubscriptionCallback, SubscriptionsNodeCache> {

    public TopicsNodeCache(CuratorFramework curatorClient, ObjectMapper objectMapper, String path, ExecutorService executorService) {
        super(curatorClient, objectMapper, path, executorService);
    }

    @Override
    protected SubscriptionsNodeCache createSubcache(String path) {
        return new SubscriptionsNodeCache(curatorClient, objectMapper, subscriptionsNodePath(path), executorService);
    }

    private String subscriptionsNodePath(String path) {
        return path + "/" + ZookeeperPaths.SUBSCRIPTIONS_PATH;
    }

    public List<SubscriptionName> listActiveSubscriptionNames() {
        return getSubcacheEntrySet().stream()
                .map(entry -> entry.getValue().listActiveSubscriptionNames())
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
