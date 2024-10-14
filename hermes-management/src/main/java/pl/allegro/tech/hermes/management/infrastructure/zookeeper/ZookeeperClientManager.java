package pl.allegro.tech.hermes.management.infrastructure.zookeeper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider;
import pl.allegro.tech.hermes.management.config.storage.StorageClustersProperties;
import pl.allegro.tech.hermes.management.config.storage.StorageProperties;

public class ZookeeperClientManager {

  private static final Logger logger = LoggerFactory.getLogger(ZookeeperClientManager.class);

  private final StorageClustersProperties properties;
  private final DatacenterNameProvider datacenterNameProvider;
  private List<ZookeeperClient> clients;
  private ZookeeperClient localClient;

  public ZookeeperClientManager(
      StorageClustersProperties properties, DatacenterNameProvider datacenterNameProvider) {
    this.properties = properties;
    this.datacenterNameProvider = datacenterNameProvider;
  }

  public void start() {
    createClients();
    selectLocalClient();
    waitForConnection(localClient.getCuratorFramework());
  }

  private void createClients() {
    clients =
        getClusterProperties().stream()
            .map(clusterProperties -> buildZookeeperClient(clusterProperties, properties))
            .collect(Collectors.toList());
  }

  private List<StorageProperties> getClusterProperties() {
    if (properties.getClusters().isEmpty()) {
      return Collections.singletonList(createPropertiesForSingleCluster());
    } else {
      return properties.getClusters();
    }
  }

  private StorageProperties createPropertiesForSingleCluster() {
    StorageProperties clusterProperties = new StorageProperties();
    clusterProperties.setConnectionString(properties.getConnectionString());
    clusterProperties.setConnectTimeout(properties.getConnectTimeout());
    clusterProperties.setSessionTimeout(properties.getSessionTimeout());
    clusterProperties.setDatacenter(DefaultDatacenterNameProvider.DEFAULT_DC_NAME);
    return clusterProperties;
  }

  private void selectLocalClient() {
    if (clients.size() == 1) {
      localClient = clients.get(0);
    } else {
      String localDcName = datacenterNameProvider.getDatacenterName();
      localClient =
          clients.stream()
              .filter(client -> client.getDatacenterName().equals(localDcName))
              .findFirst()
              .orElseThrow(() -> new ZookeeperClientNotFoundException(localDcName));
    }
  }

  private ZookeeperClient buildZookeeperClient(
      StorageProperties clusterProperties, StorageClustersProperties commonProperties) {
    return new ZookeeperClient(
        buildCuratorFramework(clusterProperties, commonProperties),
        clusterProperties.getDatacenter());
  }

  private CuratorFramework buildCuratorFramework(
      StorageProperties clusterProperties, StorageClustersProperties commonProperties) {
    ExponentialBackoffRetry retryPolicy =
        new ExponentialBackoffRetry(
            commonProperties.getRetrySleep(), commonProperties.getRetryTimes());

    CuratorFrameworkFactory.Builder builder =
        CuratorFrameworkFactory.builder()
            .connectString(clusterProperties.getConnectionString())
            .sessionTimeoutMs(clusterProperties.getSessionTimeout())
            .connectionTimeoutMs(clusterProperties.getConnectTimeout())
            .retryPolicy(retryPolicy);

    Optional.ofNullable(commonProperties.getAuthorization())
        .ifPresent(
            it -> {
              builder.authorization(
                  it.getScheme(), (it.getUser() + ":" + it.getPassword()).getBytes());
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
