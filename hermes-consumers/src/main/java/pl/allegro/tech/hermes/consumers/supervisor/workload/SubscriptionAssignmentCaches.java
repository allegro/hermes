package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

public class SubscriptionAssignmentCaches extends PathChildrenCache implements PathChildrenCacheListener {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionAssignmentCaches.class);

    private final CuratorFramework curator;
    private final String localCluster;
    private final ZookeeperPaths zookeeperPaths;
    private final SubscriptionsCache subscriptionsCache;

    private Map<String, SubscriptionAssignmentCache> caches = new ConcurrentHashMap<>();

    @Inject
    public SubscriptionAssignmentCaches(CuratorFramework curator, ConfigFactory configFactory,
                                        ZookeeperPaths zookeeperPaths, SubscriptionsCache subscriptionsCache) {
        super(curator, zookeeperPaths.consumersWorkloadPath(), true, false, createExecutor());
        this.curator = curator;
        this.localCluster = configFactory.getStringProperty(KAFKA_CLUSTER_NAME);
        this.zookeeperPaths = zookeeperPaths;
        this.subscriptionsCache = subscriptionsCache;
    }

    private static ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("cluster-cache-%d").build());
    }

    public SubscriptionAssignmentCache localClusterCache() {
        return caches.get(localCluster);
    }

    public List<SubscriptionAssignmentCache> all() {
        return new ArrayList<>(caches.values());
    }

    @PostConstruct
    public void start() throws Exception {
        getListenable().addListener(this);
        addCache(localCluster);
        super.start();
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        if (event.getData() == null) {
            return;
        }

        String path = event.getData().getPath();
        logger.info("Got {} event for path {}", event.getType(), path);

        try {
            String cluster = substringAfterLast(path, "/");
            switch (event.getType()) {
                case CHILD_ADDED:
                    addCache(cluster);
                    break;
                case CHILD_REMOVED:
                    removeCache(cluster);
                    break;
            }
        } catch (Exception e) {
            logger.error("Failed to process cluster update for event: {}", event);
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        for (SubscriptionAssignmentCache cache : caches.values()) {
            cache.stop();
        }
    }

    private void addCache(String cluster) throws Exception {
        if (!caches.containsKey(cluster)) {
            String runtimePath = zookeeperPaths.consumersRuntimePath(cluster);
            SubscriptionAssignmentCache cache = new SubscriptionAssignmentCache(
                    curator,
                    runtimePath,
                    subscriptionsCache,
                    new SubscriptionAssignmentPathSerializer(runtimePath));
            caches.put(cluster, cache);
            cache.start();
        }
    }

    private void removeCache(String cluster) throws Exception {
        SubscriptionAssignmentCache cache = caches.remove(cluster);
        if (cache != null) {
            cache.stop();
        }
    }
}
