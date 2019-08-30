package pl.allegro.tech.hermes.test.helper.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import kafka.zk.KafkaZkClient;
import kafka.zookeeper.ZooKeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.kafka.common.utils.Time;
import org.junit.AfterClass;
import org.junit.BeforeClass;

@SuppressFBWarnings("MS_PKGPROTECT")
public abstract class ZookeeperBaseTest {

    protected static TestingServer zookeeperServer;

    protected static CuratorFramework zookeeperClient;

    protected static ZookeeperWaiter wait;

    protected static KafkaZkClient kafkaZkClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected ZookeeperBaseTest() {
    }

    @BeforeClass
    public static void beforeZookeeperClass() throws Exception {
        zookeeperServer = new TestingServer(45678);
        zookeeperClient = CuratorFrameworkFactory.builder()
                .connectString(zookeeperServer.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        zookeeperClient.start();
        wait = new ZookeeperWaiter(zookeeperClient);

        wait.untilZookeeperClientStarted();

        ZooKeeperClient zooKeeperClient = new ZooKeeperClient(
                zookeeperServer.getConnectString(), 60 * 1000, 15 * 1000,
                10, Time.SYSTEM, "zookeeper-test", "zookeeper-test");
        zooKeeperClient.waitUntilConnected();
        kafkaZkClient = new KafkaZkClient(zooKeeperClient, false, Time.SYSTEM);
    }

    protected static CuratorFramework newClient() {
        CuratorFramework newClient = CuratorFrameworkFactory.builder()
                .connectString(zookeeperServer.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        newClient.start();
        wait.untilZookeeperClientStarted(newClient);
        return newClient;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        zookeeperServer.stop();
    }

    protected void createPath(String path) throws Exception {
        if (zookeeperClient.checkExists().forPath(path) == null) {
            zookeeperClient.create().creatingParentsIfNeeded().forPath(path);
        }
    }

    protected void deleteData(String path) throws Exception {
        if (zookeeperClient.checkExists().forPath(path) != null) {
            zookeeperClient.delete().deletingChildrenIfNeeded().forPath(path);
        }
    }

    protected void deleteAllNodes() throws Exception {
        if (zookeeperClient.checkExists().forPath("/hermes") != null) {
            zookeeperClient.delete().guaranteed().deletingChildrenIfNeeded().forPath("/hermes");
        }
    }

}
