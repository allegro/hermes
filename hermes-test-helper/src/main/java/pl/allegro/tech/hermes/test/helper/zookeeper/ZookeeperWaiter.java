package pl.allegro.tech.hermes.test.helper.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;


public class ZookeeperWaiter {

    private final CuratorFramework zookeeper;

    public ZookeeperWaiter(CuratorFramework zookeeper) {
        this.zookeeper = zookeeper;
    }

    public void untilZookeeperClientStarted() {
        this.untilZookeeperClientStarted(zookeeper);
    }

    public void untilZookeeperClientStarted(CuratorFramework client) {
        await().atMost(2, TimeUnit.SECONDS).until(() -> client.getState() == CuratorFrameworkState.STARTED);
    }

    public void untilZookeeperPathIsCreated(final String path) {
        await().atMost(2, TimeUnit.SECONDS).until(() -> zookeeper.getData().forPath(path) != null);
    }

    public void untilZookeeperPathNotExists(final String path) {
        await().atMost(2, TimeUnit.SECONDS).until(() -> zookeeper.checkExists().forPath(path) == null);
    }
}
