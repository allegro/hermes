package pl.allegro.tech.hermes.test.helper.environment;

import com.google.common.io.Files;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.InstanceSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;

public class ZookeeperStarter implements Starter<FixedHostPortGenericContainer> {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperStarter.class);

    private final FixedHostPortGenericContainer zookeeperServer;

    private final int port;
    private final String connectString;
    private final String[] pathsToInitialize;

    public ZookeeperStarter(Network network, int port, String connectString, String... pathsToInitialize) {
        this.port = port;
        this.connectString = connectString;
        this.pathsToInitialize = pathsToInitialize;
        this.zookeeperServer = new FixedHostPortGenericContainer<>("confluentinc/cp-zookeeper")
                .withNetwork(network)
                .withFixedExposedPort(port, port)
                .withExposedPorts(port)
                .withEnv("ZOOKEEPER_CLIENT_PORT", String.valueOf(port))
                .withCreateContainerCmdModifier(it -> it.withName("zookeeper"))
                .waitingFor(Wait.defaultWaitStrategy());
    }

    @Override
    public void start() throws Exception {
        logger.info("Running Zookeeper in a docker container at port {}", port);
        zookeeperServer.start();

        String[] zkConnectStringSplitted = connectString.split("/", 2);

        try(CuratorFramework curator = startZookeeperClient(zkConnectStringSplitted[0])) {
            if (zkConnectStringSplitted.length > 1) {
                curator.create().creatingParentsIfNeeded().forPath("/" + zkConnectStringSplitted[1]);
            }
            for(String path : pathsToInitialize) {
                curator.create().creatingParentsIfNeeded().forPath(path);
            }
        }
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping Zookeeper");
        zookeeperServer.stop();
    }

    @Override
    public FixedHostPortGenericContainer instance() {
        return zookeeperServer;
    }

    private InstanceSpec config(int port) {
        File dataDirectory = Files.createTempDir();
        int electionPort = -1; //negative value means use default value
        int quorumPort = -1;
        boolean deleteDataDirectoryOnClose = true;
        int serverId = -1;
        int tickTime = -1;
        int maxClientCnxns = 1000;

        return new InstanceSpec(
                dataDirectory,
                port,
                electionPort,
                quorumPort,
                deleteDataDirectoryOnClose,
                serverId,
                tickTime,
                maxClientCnxns);
    }

    private CuratorFramework startZookeeperClient(String connectString) throws InterruptedException {
        CuratorFramework zookeeperClient = CuratorFrameworkFactory.builder()
            .connectString(connectString)
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .build();
        zookeeperClient.start();
        zookeeperClient.blockUntilConnected();
        return zookeeperClient;
    }
}
