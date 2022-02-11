package pl.allegro.tech.hermes.infrastructure.zookeeper.cache;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.cache.queue.LinkedHashSetBlockingQueue;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ModelAwareZookeeperNotifyingCache {

    private static final Logger logger = LoggerFactory.getLogger(ModelAwareZookeeperNotifyingCache.class);

    private static final int GROUP_LEVEL = 0;

    private static final int TOPIC_LEVEL = 1;

    private static final int SUBSCRIPTION_LEVEL = 2;

    private final HierarchicalCache cache;
    private final ThreadPoolExecutor executor;

    public ModelAwareZookeeperNotifyingCache(CuratorFramework curator, String rootPath, int processingThreadPoolSize) {
        List<String> levelPrefixes = Arrays.asList(
                ZookeeperPaths.GROUPS_PATH, ZookeeperPaths.TOPICS_PATH, ZookeeperPaths.SUBSCRIPTIONS_PATH
        );
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(rootPath + "-zk-cache-%d").build();
        executor = new ThreadPoolExecutor(1, processingThreadPoolSize,
                Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedHashSetBlockingQueue<>(), threadFactory);
        this.cache = new HierarchicalCache(
                curator,
                executor,
                rootPath,
                3,
                levelPrefixes,
                true
        );
    }

    public void start() throws Exception {
        cache.start();
    }

    public void stop() {
        try {
            cache.stop();
            executor.shutdownNow();
        } catch (Exception e) {
            logger.warn("Failed to stop Zookeeper cache", e);
        }
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
