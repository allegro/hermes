package pl.allegro.tech.hermes.test.helper.zookeeper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

@SuppressFBWarnings("MS_PKGPROTECT")
public abstract class ZookeeperBaseTest {

    protected static TestingServer zookeeperServer;

    protected static CuratorFramework zookeeperClient;

    protected static ZookeeperWaiter wait;

    protected ZookeeperBaseTest() {
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        zookeeperServer = new TestingServer(45678);
        zookeeperClient = CuratorFrameworkFactory.builder()
                .connectString(zookeeperServer.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        zookeeperClient.start();
        wait = new ZookeeperWaiter(zookeeperClient);

        wait.untilZookeeperClientStarted();
    }

    protected static CuratorFramework otherClient() {
        CuratorFramework otherClient = CuratorFrameworkFactory.builder()
                .connectString(zookeeperServer.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        otherClient.start();
        wait.untilZookeeperClientStarted(otherClient);
        return otherClient;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        zookeeperServer.stop();
    }

    public void deleteData(String path) throws Exception {
        if (zookeeperClient.checkExists().forPath(path) != null) {
            zookeeperClient.delete().deletingChildrenIfNeeded().forPath(path);
        }
    }

    public void deleteAllNodes() throws Exception {
        zookeeperClient.delete().guaranteed().deletingChildrenIfNeeded().forPath("/hermes");
    }

}
