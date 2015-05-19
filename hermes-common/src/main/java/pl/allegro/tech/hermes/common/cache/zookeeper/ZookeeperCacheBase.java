package pl.allegro.tech.hermes.common.cache.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.EnsurePath;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class ZookeeperCacheBase {

    protected final ExecutorService executorService;
    protected final ZookeeperPaths paths;
    private final CuratorFramework curatorClient;

    public ZookeeperCacheBase(ConfigFactory configFactory, CuratorFramework curatorClient) {
        this.curatorClient = curatorClient;

        executorService = Executors.newFixedThreadPool(configFactory.getIntProperty(Configs.ZOOKEEPER_CACHE_THREAD_POOL_SIZE));
        paths = new ZookeeperPaths(configFactory.getStringProperty(Configs.ZOOKEEPER_ROOT));
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

    protected interface StartFunction {
        void start() throws Exception;
    }
}
