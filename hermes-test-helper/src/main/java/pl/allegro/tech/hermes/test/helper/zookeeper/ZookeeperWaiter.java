package pl.allegro.tech.hermes.test.helper.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

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

    public void untilZookeeperPathIsCreated(String... path) {
        untilZookeeperPathIsCreated(stream(path).collect(joining("/")));
    }

    public void untilZookeeperPathIsEmpty(final String path) {
        await().atMost(2, TimeUnit.SECONDS).until(() -> zookeeper.getChildren().forPath(path).isEmpty());
    }

    public void untilZookeeperPathIsEmpty(String... path) {
        untilZookeeperPathIsEmpty(stream(path).collect(joining("/")));
    }

    public void untilZookeeperPathNotExists(final String path) {
        await().atMost(2, TimeUnit.SECONDS).until(() -> zookeeper.checkExists().forPath(path) == null);
    }

    public void untilZookeeperPathNotExists(String... path) {
        untilZookeeperPathNotExists(stream(path).collect(joining("/")));
    }
}
