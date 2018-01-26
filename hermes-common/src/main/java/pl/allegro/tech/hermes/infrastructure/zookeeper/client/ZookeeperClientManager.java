package pl.allegro.tech.hermes.infrastructure.zookeeper.client;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc.DcNameProvider;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ZookeeperClientManager {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperClientManager.class);

    private final ZookeeperProperties properties;
    private final DcNameProvider dcNameProvider;
    private List<ZookeeperClient> clients;
    private String localDcName;

    public ZookeeperClientManager(ZookeeperProperties properties, DcNameProvider dcNameProvider) {
        this.properties = properties;
        this.dcNameProvider = dcNameProvider;
    }

    public void start() {
        localDcName = dcNameProvider.getDcName();
        clients = properties.getClusters()
                        .stream()
                        .map(clusterProperties -> buildZookeeperClient(clusterProperties, properties))
                        .collect(Collectors.toList());
    }

    private ZookeeperClient buildZookeeperClient(ZookeeperClusterProperties clusterProperties,
                                                 ZookeeperProperties commonProperties) {
        return new ZookeeperClient(
                buildCuratorFramework(clusterProperties, commonProperties),
                clusterProperties.getName(),
                clusterProperties.getDc()
        );
    }

    private CuratorFramework buildCuratorFramework(ZookeeperClusterProperties clusterProperties,
                                                   ZookeeperProperties commonProperties) {
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
        for(ZookeeperClient client : clients) {
            try {
                client.getCuratorFramework().close();
            } catch (Exception e) {
                logger.warn("Failed to close Zookeeper client: " + client.getName());
            }
        }
    }

    public ZookeeperClient getLocalClient() {
        Optional<ZookeeperClient> localClient = clients
                .stream()
                .filter(client -> client.isDeployedOnDc(localDcName))
                .findFirst();

        if(!localClient.isPresent()) {
            throw new ZookeeperClientNotFoundException("No Zookeeper client is configured to connect " +
                    "to cluster on DC (name: " + localDcName + ").");
        }

        return localClient.get();
    }

    public List<ZookeeperClient> getClients() {
        return clients;
    }
}
