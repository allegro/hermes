package pl.allegro.tech.hermes.common.cache.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.EnsurePath;
import pl.allegro.tech.hermes.common.cache.queue.LinkedHashSetBlockingQueue;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.common.config.Configs.ZOOKEEPER_CACHE_THREAD_POOL_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.ZOOKEEPER_TASK_PROCESSING_THREAD_POOL_SIZE;

public abstract class ZookeeperCacheBase {

    protected final ExecutorService eventExecutor;
    protected final ExecutorService processingExecutor;
    protected final ZookeeperPaths paths;
    private final CuratorFramework curatorClient;

    public ZookeeperCacheBase(ConfigFactory configFactory, CuratorFramework curatorClient) {
        this.curatorClient = curatorClient;
        this.eventExecutor = Executors.newFixedThreadPool(configFactory.getIntProperty(ZOOKEEPER_CACHE_THREAD_POOL_SIZE));
        this.processingExecutor = new ThreadPoolExecutor(1, configFactory.getIntProperty(ZOOKEEPER_TASK_PROCESSING_THREAD_POOL_SIZE),
                Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedHashSetBlockingQueue<>());
        this.paths = new ZookeeperPaths(configFactory.getStringProperty(Configs.ZOOKEEPER_ROOT));
    }

    protected void checkBasePath(StartFunction startFunction) {
        ensureBasePath();
        try {
            startFunction.start();
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    private void ensureBasePath() {
        try {
            new EnsurePath(paths.groupsPath()).ensure(this.curatorClient.getZookeeperClient());
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public void stop() {
        eventExecutor.shutdown();
        processingExecutor.shutdown();
    }

    protected interface StartFunction {
        void start() throws Exception;
    }
}
