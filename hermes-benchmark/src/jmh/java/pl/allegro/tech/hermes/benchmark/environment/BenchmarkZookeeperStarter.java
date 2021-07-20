package pl.allegro.tech.hermes.benchmark.environment;


import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

public class BenchmarkZookeeperStarter implements Starter<TestingServer> {

    private static final Logger logger = LoggerFactory.getLogger(pl.allegro.tech.hermes.test.helper.environment.ZookeeperStarter.class);

    private TestingServer zookeeperServer;

    public BenchmarkZookeeperStarter() {

    }

    public String getConnectString() {
        return String.format("localhost:%s", zookeeperServer.getPort());
    }

    @Override
    public void start() throws Exception {
        zookeeperServer = new TestingServer(true);
        int port = zookeeperServer.getPort();
        logger.info("Running in-memory Zookeeper at port {}", port);

        try(CuratorFramework curator = startZookeeperClient(getConnectString())) {
            curator.createContainers("hermes/groups");
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