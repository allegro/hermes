package pl.allegro.tech.hermes.management.infrastructure.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.management.config.storage.StorageClustersProperties;
import pl.allegro.tech.hermes.management.config.storage.StorageProperties;
import pl.allegro.tech.hermes.management.infrastructure.dc.DcNameProvider;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ZookeeperClientManager {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperClientManager.class);

    private final StorageClustersProperties properties;
    private final DcNameProvider dcNameProvider;
    private List<ZookeeperClient> clients;
    private ZookeeperClient localClient;

    public ZookeeperClientManager(StorageClustersProperties properties, DcNameProvider dcNameProvider) {
        this.properties = properties;
        this.dcNameProvider = dcNameProvider;
    }

    public void start() {
        createClients();
        selectLocalClient();
    }

    private void createClients() {
        clients = properties.getClusters()
                .stream()
                .map(clusterProperties -> buildZookeeperClient(clusterProperties, properties))
                .collect(Collectors.toList());
    }

    private void selectLocalClient() {
        String localDcName = dcNameProvider.getDcName();
        localClient = clients
                .stream()
                .filter(client -> client.getDcName().equals(localDcName))
                .findFirst()
                .orElseThrow(() -> new ZookeeperClientNotFoundException(localDcName));
    }

    private ZookeeperClient buildZookeeperClient(StorageProperties clusterProperties,
                                                 StorageClustersProperties commonProperties) {
        return new ZookeeperClient(
                buildCuratorFramework(clusterProperties, commonProperties),
                clusterProperties.getDc()
        );
    }

    private CuratorFramework buildCuratorFramework(StorageProperties clusterProperties,
                                                   StorageClustersProperties commonProperties) {
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(commonProperties.getRetrySleep(),
                commonProperties.getRetryTimes());

        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(clusterProperties.getConnectionString())
                .sessionTimeoutMs(clusterProperties.getSessionTimeout())
                .connectionTimeoutMs(clusterProperties.getConnectTimeout())
                .retryPolicy(retryPolicy);

        Optional.ofNullable(commonProperties.getAuthorization()).ifPresent(it ->
                builder.authorization(it.getScheme(), (it.getUser() + ":" + it.getPassword()).getBytes())
        );

        CuratorFramework curator = builder.build();

        startAndWaitForConnection(curator);

        return curator;
    }

    private void startAndWaitForConnection(CuratorFramework curator) {
        curator.start();
        try {
            curator.blockUntilConnected();
        } catch (InterruptedException interruptedException) {
            RuntimeException exception = new InternalProcessingException("Could not start curator for storage",
                    interruptedException);
            logger.error(exception.getMessage(), interruptedException);
            throw exception;
        }
    }

    public void stop() {
        for (ZookeeperClient client : clients) {
            try {
                client.getCuratorFramework().close();
            } catch (Exception e) {
                logger.warn("Failed to close Zookeeper client on DC: " + client.getDcName());
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
