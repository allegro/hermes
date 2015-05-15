package pl.allegro.tech.hermes.common.cache.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.EnsurePath;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.config.zookeeper.NodePassword;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class ZookeeperCacheBase {

    protected final ExecutorService executorService;
    protected final ZookeeperPaths paths;
    private final ConfigFactory configFactory;
    private final CuratorFramework curatorClient;
    private final ObjectMapper objectMapper;

    public ZookeeperCacheBase(ConfigFactory configFactory, CuratorFramework curatorClient, ObjectMapper objectMapper) {
        this.configFactory = configFactory;
        this.curatorClient = curatorClient;
        this.objectMapper = objectMapper;

        executorService = Executors.newFixedThreadPool(configFactory.getIntProperty(Configs.ZOOKEEPER_CACHE_THREAD_POOL_SIZE));
        paths = new ZookeeperPaths(configFactory.getStringProperty(Configs.ZOOKEEPER_ROOT));
    }

    protected void checkBasePath(StartFunction startFunction) {
        ensureBasePath();
        try {
            setupAdminPassword();
            startFunction.start();
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }


    private void setupAdminPassword() throws Exception {
        curatorClient.setData().forPath(paths.groupsPath(), objectMapper.writeValueAsBytes(
                NodePassword.fromString(configFactory.getStringProperty(Configs.ADMIN_PASSWORD))));
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
