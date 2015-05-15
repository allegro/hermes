package pl.allegro.tech.hermes.consumers.subscription.cache.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.cache.zookeeper.NodeCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionCallback;

import java.util.concurrent.ExecutorService;

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

}
