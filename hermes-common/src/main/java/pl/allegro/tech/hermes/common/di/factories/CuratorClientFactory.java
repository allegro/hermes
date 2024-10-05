package pl.allegro.tech.hermes.common.di.factories;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

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

  public CuratorFramework provide(
      String connectString, Optional<ZookeeperAuthorization> zookeeperAuthorization) {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder()
            .setNameFormat("hermes-curator-%d")
            .setUncaughtExceptionHandler(
                (t, e) -> logger.error("Exception from curator with name {}", t.getName(), e))
            .build();
    CuratorFrameworkFactory.Builder builder =
        CuratorFrameworkFactory.builder()
            .threadFactory(threadFactory)
            .connectString(connectString)
            .sessionTimeoutMs((int) zookeeperParameters.getSessionTimeout().toMillis())
            .connectionTimeoutMs((int) zookeeperParameters.getConnectionTimeout().toMillis())
            .retryPolicy(
                new ExponentialBackoffRetry(
                    (int) zookeeperParameters.getBaseSleepTime().toMillis(),
                    zookeeperParameters.getMaxRetries(),
                    (int) zookeeperParameters.getMaxSleepTime().toMillis()));

    zookeeperAuthorization.ifPresent(
        it -> {
          builder.authorization(it.scheme, it.getAuth());
          builder.aclProvider(
              new ACLProvider() {
                @Override
                public List<ACL> getDefaultAcl() {
                  return ZooDefs.Ids.CREATOR_ALL_ACL;
                }

                @Override
                public List<ACL> getAclForPath(String path) {
                  return ZooDefs.Ids.CREATOR_ALL_ACL;
                }
              });
        });

    CuratorFramework curatorClient = builder.build();
    startAndWaitForConnection(curatorClient);

    return curatorClient;
  }

  private void startAndWaitForConnection(CuratorFramework curator) {
    curator.start();
    try {
      curator.blockUntilConnected();
    } catch (InterruptedException interruptedException) {
      RuntimeException exception =
          new InternalProcessingException(
              "Could not start Zookeeper Curator", interruptedException);
      logger.error(exception.getMessage(), interruptedException);
      throw exception;
    }
  }
}
