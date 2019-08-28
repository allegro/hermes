package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import java.util.List;
import java.util.Optional;

class ZookeeperOperations {

    private final CuratorFramework curator;

    ZookeeperOperations(CuratorFramework curator) {
        this.curator = curator;
    }

    void writeOrCreatePersistent(String path, byte[] serializedData) throws Exception {
        try {
            curator.setData().forPath(path, serializedData);
        } catch (KeeperException.NoNodeException e) {
            try {
                curator.create().creatingParentContainersIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(path, serializedData);
            } catch (KeeperException.NodeExistsException ex) {
                // ignore
            }
        }
    }

    Optional<byte[]> getNodeData(String path) {
        try {
            if (curator.checkExists().forPath(path) != null) {
                return Optional.of(curator.getData().forPath(path));
            }
        } catch (Exception e) {
            throw new InternalProcessingException(String.format("Could not read node data on path %s", path), e);
        }
        return Optional.empty();
    }

    void deleteNodeRecursively(String path) {
        try {
            if (curator.checkExists().forPath(path) != null) {
                curator.delete().deletingChildrenIfNeeded().forPath(path);
            }
        } catch (Exception e) {
            throw new InternalProcessingException("Could not delete node " + path, e);
        }
    }

    List<String> getNodeChildren(String path) {
        try {
            return curator.getChildren().forPath(path);
        } catch (Exception e) {
            throw new InternalProcessingException("Could not get children of node " + path, e);
        }
    }

    boolean exists(String path) {
        try {
            return curator.checkExists().forPath(path) != null;
        } catch (Exception e) {
            throw new InternalProcessingException("Unable to check existence of node " + path, e);
        }
    }
}
