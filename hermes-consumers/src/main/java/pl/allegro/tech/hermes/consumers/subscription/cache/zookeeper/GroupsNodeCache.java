package pl.allegro.tech.hermes.consumers.subscription.cache.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.cache.zookeeper.NodeCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionCallback;

import java.util.concurrent.ExecutorService;

class GroupsNodeCache extends NodeCache<SubscriptionCallback, TopicsNodeCache> {

    public GroupsNodeCache(CuratorFramework curatorClient, ObjectMapper objectMapper, String path, ExecutorService executorService) {
        super(curatorClient, objectMapper, path, executorService);
    }

    @Override
    protected TopicsNodeCache createSubcache(String path) {
        return new TopicsNodeCache(curatorClient, objectMapper, topicsNodePath(path), executorService);
    }

    private String topicsNodePath(String path) {
        return path + "/" + ZookeeperPaths.TOPICS_PATH;
    }

}
