package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import javax.inject.Inject;
import java.util.Optional;

public class CuratorClientFactory {

    public static class ZookeeperAuthorization {
        private final String scheme;
        private final String user;
        private final String password;

        public ZookeeperAuthorization(String scheme, String user, String password) {
            this.scheme = scheme;
            this.user = user;
            this.password = password;
        }

        byte[] getAuth() {
            return String.join(":", user, password).getBytes();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(CuratorClientFactory.class);
    private final ConfigFactory configFactory;

    @Inject
    public CuratorClientFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    public CuratorFramework provide(String connectString) {
        return provide(connectString, Optional.empty());
    }

    public CuratorFramework provide(String connectString, Optional<ZookeeperAuthorization> zookeeperAuthorization) {
        int baseSleepTime = configFactory.getIntProperty(Configs.ZOOKEEPER_BASE_SLEEP_TIME);
        int maxRetries = configFactory.getIntProperty(Configs.ZOOKEEPER_MAX_RETRIES);
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .sessionTimeoutMs(configFactory.getIntProperty(Configs.ZOOKEEPER_SESSION_TIMEOUT))
                .connectionTimeoutMs(configFactory.getIntProperty(Configs.ZOOKEEPER_CONNECTION_TIMEOUT))
                .retryPolicy(new ExponentialBackoffRetry(baseSleepTime, maxRetries));

        zookeeperAuthorization.ifPresent(it -> builder.authorization(it.scheme, it.getAuth()));

        CuratorFramework curatorClient = builder.build();
        startAndWaitForConnection(curatorClient);

        return curatorClient;
    }

    private void startAndWaitForConnection(CuratorFramework curator) {
        curator.start();
        try {
            curator.blockUntilConnected();
        } catch (InterruptedException interruptedException) {
            RuntimeException exception = new InternalProcessingException("Could not start Zookeeper Curator", interruptedException);
            logger.error(exception.getMessage(), interruptedException);
            throw exception;
        }
    }
}
