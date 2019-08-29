package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.workload.constraints.ConsumersWorkloadConstraints;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ZookeeperWorkloadConstraintsCache extends PathChildrenCache implements PathChildrenCacheListener {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperWorkloadConstraintsCache.class);

    private final Map<TopicName, Constraints> topicConstraintsCache = new ConcurrentHashMap<>();
    private final Map<SubscriptionName, Constraints> subscriptionConstraintsCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ZookeeperPaths paths;

    ZookeeperWorkloadConstraintsCache(CuratorFramework curatorFramework, ZookeeperPaths paths) {
        super(curatorFramework, paths.consumersWorkloadConstraintsPath(), true);
        this.paths = paths;
        getListenable().addListener(this);
    }

    ConsumersWorkloadConstraints getConsumersWorkloadConstraints() {
        return new ConsumersWorkloadConstraints(topicConstraintsCache, subscriptionConstraintsCache);
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) {
        switch (event.getType()) {
            case CHILD_ADDED:
                updatedCache(event.getData().getPath(), event.getData().getData());
                break;
            case CHILD_REMOVED:
                removeFromCache(event.getData().getPath());
                break;
            case CHILD_UPDATED:
                updatedCache(event.getData().getPath(), event.getData().getData());
                break;
            default:
                break;
        }
    }

    private void updatedCache(String path, byte[] bytes) {
        Constraints constraints = bytesToConstraints(bytes, path);
        if (constraints == null) {
            return;
        }
        if (isSubscription(path)) {
            subscriptionConstraintsCache.put(
                    SubscriptionName.fromString(paths.extractChildNode(path, paths.consumersWorkloadConstraintsPath())),
                    constraints);
        } else {
            topicConstraintsCache.put(
                    TopicName.fromQualifiedName(paths.extractChildNode(path, paths.consumersWorkloadConstraintsPath())),
                    constraints);
        }
    }

    private Constraints bytesToConstraints(byte[] bytes, String path) {
        try {
            return objectMapper.readValue(bytes, Constraints.class);
        } catch (Exception e) {
            logger.error("Cannot read data from node: {}", path, e);
            return null;
        }
    }

    private void removeFromCache(String path) {
        if (isSubscription(path)) {
            subscriptionConstraintsCache.remove(SubscriptionName.fromString(paths.extractChildNode(path, "/hermes/consumers-workload-constraints")));
        } else {
            topicConstraintsCache.remove(TopicName.fromQualifiedName(paths.extractChildNode(path, "/hermes/consumers-workload-constraints")));
        }
    }

    private boolean isSubscription(String path) {
        return path.contains("$");
    }
}
