package pl.allegro.tech.hermes.common.di.factories;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import java.util.Optional;
import java.util.concurrent.ThreadFactory;

import static java.time.Duration.ofSeconds;

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
    private final ZookeeperParameters zookeeperParameters;

    public CuratorClientFactory(ZookeeperParameters zookeeperParameters) {
        this.zookeeperParameters = zookeeperParameters;
    }

    public CuratorFramework provide(String connectString) {
        return provide(connectString, Optional.empty());
    }

    public CuratorFramework provide(String connectString, Optional<ZookeeperAuthorization> zookeeperAuthorization) {
        int baseSleepTime = zookeeperParameters.getBaseSleepTime();
        int maxRetries = zookeeperParameters.getMaxRetries();
        int maxSleepTime = Ints.saturatedCast(ofSeconds(zookeeperParameters.getMaxSleepTimeSeconds()).toMillis());
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("hermes-curator-%d")
                .setUncaughtExceptionHandler((t, e) ->
                        logger.error("Exception from curator with name {}", t.getName(), e)).build();
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .threadFactory(threadFactory)
                .connectString(connectString)
                .sessionTimeoutMs(zookeeperParameters.getSessionTimeout())
                .connectionTimeoutMs(zookeeperParameters.getConnectionTimeout())
                .retryPolicy(new ExponentialBackoffRetry(baseSleepTime, maxRetries, maxSleepTime));

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
