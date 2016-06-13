package pl.allegro.tech.hermes.infrastructure.zookeeper.cache;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.utils.EnsurePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class HierarchicalCache {

    private static final Logger logger = LoggerFactory.getLogger(HierarchicalCache.class);

    private final CuratorFramework curatorFramework;

    private final ExecutorService executorService;

    private final String basePath;

    private final List<String> levelPrefixes = new ArrayList<>();

    private final int maxDepth;

    private final List<CacheListeners> levelCallbacks = new ArrayList<>();

    private HierarchicalCacheLevel rootCache;

    public HierarchicalCache(CuratorFramework curatorFramework,
                             ExecutorService executorService,
                             String basePath,
                             int maxDepth,
                             List<String> levelPrefixes) {
        this.curatorFramework = curatorFramework;
        this.executorService = executorService;
        this.basePath = basePath;
        this.levelPrefixes.addAll(levelPrefixes);
        this.maxDepth = maxDepth;

        for(int i = 0; i < maxDepth; ++i) {
            levelCallbacks.add(new CacheListeners());
        }
    }

    public void start() throws Exception {
        ensureBasePath();
        rootCache = createLevelCache(0, basePath);
    }

    public void stop() throws Exception {
        rootCache.stop();
    }

    public void registerCallback(int depth, Consumer<PathChildrenCacheEvent> callback) {
        levelCallbacks.get(depth).addListener(callback);
    }

    private HierarchicalCacheLevel createLevelCache(int depth, String path) {
        BiFunction<Integer, String, HierarchicalCacheLevel> function = depth + 1 < maxDepth ? this::createLevelCache : null;
        HierarchicalCacheLevel levelCache = new HierarchicalCacheLevel(curatorFramework,
                executorService,
                path(depth, path),
                depth,
                levelCallbacks.get(depth),
                Optional.ofNullable(function));
        try {
            levelCache.start();
        } catch (Exception e) {
            logger.error("Failed to start hierarchical cache level {} for path {}", depth, path(depth, path), e);
        }
        return levelCache;
    }

    private String path(int depth, String basePath) {
        return basePath + (levelPrefixes.size() > depth ? "/" + levelPrefixes.get(depth) : "");
    }

    private void ensureBasePath() {
        try {
            new EnsurePath(path(0, basePath));
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }
}
