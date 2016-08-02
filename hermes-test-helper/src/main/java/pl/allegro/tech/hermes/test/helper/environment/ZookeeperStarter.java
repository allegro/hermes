package pl.allegro.tech.hermes.test.helper.environment;

import com.google.common.io.Files;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ZookeeperStarter implements Starter<TestingServer> {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperStarter.class);

    private TestingServer zookeeperServer;

    private final int port;
    private final String connectString;
    private final String[] pathsToInitialize;

    public ZookeeperStarter(int port, String connectString, String... pathsToInitialize) {
        this.port = port;
        this.connectString = connectString;
        this.pathsToInitialize = pathsToInitialize;
    }

    @Override
    public void start() throws Exception {
        logger.info("Running in-memory Zookeeper at port {}", port);
        zookeeperServer = new TestingServer(config(port), true);

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
        logger.info("Stopping in-memory Zookeeper");
        zookeeperServer.close();
    }

    @Override
    public TestingServer instance() {
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