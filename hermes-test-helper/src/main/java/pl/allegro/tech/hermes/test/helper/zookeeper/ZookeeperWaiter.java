package pl.allegro.tech.hermes.test.helper.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

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

    public void untilZookeeperClientStopped() {
        this.untilZookeeperClientStopped(zookeeper);
    }

    public void untilZookeeperClientStopped(CuratorFramework client) {
        await().atMost(2, TimeUnit.SECONDS).until(() -> client.getState() == CuratorFrameworkState.STOPPED);
    }

    public void untilZookeeperPathIsCreated(final String path) {
        await().atMost(2, TimeUnit.SECONDS).until(() -> zookeeper.getData().forPath(path) != null);
    }

    public void untilZookeeperPathIsEmpty(final String path) {
        await().atMost(2, TimeUnit.SECONDS).until(() -> zookeeper.getChildren().forPath(path).isEmpty());
    }
}
