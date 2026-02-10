package pl.allegro.tech.hermes.management.infrastructure.zookeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.di.factories.ZookeeperParameters;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

public class ZookeeperClientManager {

  private static final Logger logger = LoggerFactory.getLogger(ZookeeperClientManager.class);

  private final List<ZookeeperParameters> zookeeperParameters;
  private final DatacenterNameProvider datacenterNameProvider;
  private List<ZookeeperClient> clients;
  private ZookeeperClient localClient;

  public ZookeeperClientManager(
          List<? extends ZookeeperParameters> zookeeperParameters, DatacenterNameProvider datacenterNameProvider) {
    this.zookeeperParameters = new ArrayList<>(zookeeperParameters);
    this.datacenterNameProvider = datacenterNameProvider;
  }

  public void start() {
    createClients();
    selectLocalClient();
    waitForConnection(localClient.getCuratorFramework());
  }

  private void createClients() {
    clients =
        zookeeperParameters.stream()
            .map(this::buildZookeeperClient)
            .collect(Collectors.toList());
  }

  private void selectLocalClient() {
    if (clients.size() == 1) {
      localClient = clients.getFirst();
    } else {
      String localDcName = datacenterNameProvider.getDatacenterName();
      localClient =
          clients.stream()
              .filter(client -> client.getDatacenterName().equals(localDcName))
              .findFirst()
              .orElseThrow(() -> new ZookeeperClientNotFoundException(localDcName));
    }
  }

  private ZookeeperClient buildZookeeperClient(ZookeeperParameters parameters) {
    return new ZookeeperClient(
        buildCuratorFramework(parameters),
        parameters.getDatacenter());
  }

  private CuratorFramework buildCuratorFramework(ZookeeperParameters parameters) {
    ExponentialBackoffRetry retryPolicy =
        new ExponentialBackoffRetry(
                (int) parameters.getBaseSleepTime().toMillis(), parameters.getMaxRetries());

    CuratorFrameworkFactory.Builder builder =
        CuratorFrameworkFactory.builder()
            .connectString(parameters.getConnectionString())
            .sessionTimeoutMs((int) parameters.getSessionTimeout().toMillis())
            .connectionTimeoutMs((int) parameters.getConnectionTimeout().toMillis())
            .retryPolicy(retryPolicy);

    if (parameters.isAuthenticationEnabled()) {
      builder.authorization(
          parameters.getScheme(), (parameters.getUser() + ":" + parameters.getPassword()).getBytes());
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
    }

    CuratorFramework curator = builder.build();
    curator.start();

    return curator;
  }

  private void waitForConnection(CuratorFramework curator) {
    try {
      curator.blockUntilConnected();
    } catch (InterruptedException interruptedException) {
      RuntimeException exception =
          new InternalProcessingException(
              "Could not start curator for storage", interruptedException);
      logger.error(exception.getMessage(), interruptedException);
      throw exception;
    }
  }

  public void stop() {
    for (ZookeeperClient client : clients) {
      try {
        client.getCuratorFramework().close();
      } catch (Exception e) {
        logger.warn("Failed to close Zookeeper client on DC: " + client.getDatacenterName());
      }
    }
  }

  public ZookeeperClient getLocalClient() {
    return localClient;
  }

  public List<ZookeeperClient> getClients() {
    return clients;
  }
}

