package pl.allegro.tech.hermes.infrastructure.zookeeper.cache;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import pl.allegro.tech.hermes.common.cache.queue.LinkedHashSetBlockingQueue;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ModelAwareZookeeperNotifyingCache {

    private static final int GROUP_LEVEL = 0;

    private static final int TOPIC_LEVEL = 1;

    private static final int SUBSCRIPTION_LEVEL = 2;

    private final HierarchicalCache cache;

    public ModelAwareZookeeperNotifyingCache(CuratorFramework curator, String rootPath, int processingThreadPoolSize) {
        List<String> levelPrefixes = Arrays.asList(
                ZookeeperPaths.GROUPS_PATH, ZookeeperPaths.TOPICS_PATH, ZookeeperPaths.SUBSCRIPTIONS_PATH
        );

        this.cache = new HierarchicalCache(
                curator,
                new ThreadPoolExecutor(1, processingThreadPoolSize,
                        Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedHashSetBlockingQueue<>()),
                rootPath,
                3,
                levelPrefixes
        );
    }

    public void start() throws Exception {
        cache.start();
    }

    public void stop() throws Exception {
        cache.stop();
    }

    public void registerGroupCallback(Consumer<PathChildrenCacheEvent> callback) {
        cache.registerCallback(GROUP_LEVEL, callback);
    }


    public void registerTopicCallback(Consumer<PathChildrenCacheEvent> callback) {
        cache.registerCallback(TOPIC_LEVEL, callback);
    }

    public void registerSubscriptionCallback(Consumer<PathChildrenCacheEvent> callback) {
        cache.registerCallback(SUBSCRIPTION_LEVEL, callback);
    }
}
