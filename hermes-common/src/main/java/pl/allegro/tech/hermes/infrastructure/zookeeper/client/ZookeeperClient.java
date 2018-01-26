package pl.allegro.tech.hermes.infrastructure.zookeeper.client;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ZookeeperClient {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperClient.class);

    private final CuratorFramework curatorFramework;
    private final String name;
    private final String dc;

    public ZookeeperClient(CuratorFramework curatorFramework, String name, String dc) {
        this.curatorFramework = curatorFramework;
        this.name = name;
        this.dc = dc;
    }

    public CuratorFramework getCuratorFramework() {
        return curatorFramework;
    }

    public String getDc() {
        return dc;
    }

    public String getName() {
        return name;
    }

    public boolean isDeployedOnDc(String dcName) {
        return dcName.equals(dc);
    }

    public byte[] getData(String path) {
        ensureConnected();
        try {
            return curatorFramework.getData().forPath(path);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public boolean pathExists(String path) {
        ensureConnected();
        try {
            return curatorFramework.checkExists().forPath(path) != null;
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public boolean isPathEmpty(String path) {
        ensureConnected();
        try {
            byte[] data = curatorFramework.getData().forPath(path);
            return data.length == 0;
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public void touch(String path) {
        ensureConnected();
        try {
            byte[] oldData = curatorFramework.getData().forPath(path);
            curatorFramework.setData().forPath(path, oldData);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public List<String> childrenOf(String path) {
        ensureConnected();
        try {
            List<String> retrievedNodes = new ArrayList<>(curatorFramework.getChildren().forPath(path));
            Collections.sort(retrievedNodes);
            return retrievedNodes;
        } catch (KeeperException.NoNodeException e) {
            return Collections.emptyList();
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public <T> Optional<T> readFrom(String path, ThrowingReader<T> reader, boolean quiet) {
        ensureConnected();
        try {
            byte[] data = curatorFramework.getData().forPath(path);
            return Optional.of(reader.read(data));
        } catch (Exception e) {
            if (quiet) {
                logger.warn("Unable to read data from path {}", path, e);
                return Optional.empty();
            } else {
                throw new InternalProcessingException(e);
            }
        }
    }

    public void setData(String path, byte[] data) {
        ensureConnected();
        try {
            curatorFramework.setData().forPath(path, data);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public void create(String path, byte[] data) {
        ensureConnected();
        try {
            curatorFramework.create().forPath(path, data);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public void ensurePathExists(String path) {
        ensureConnected();
        if (!pathExists(path)) {
            try {
                curatorFramework.create().creatingParentsIfNeeded().forPath(path);
            } catch (Exception e) {
                throw new InternalProcessingException(e);
            }
        }
    }

    public void deleteWithChildren(String path) {
        ensureConnected();
        try {
            curatorFramework.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public void deleteWithChildrenWithGuarantee(String path) {
        ensureConnected();
        try {
            curatorFramework.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public void ensureConnected() {
        if (!curatorFramework.getZookeeperClient().isConnected()) {
            throw new InternalProcessingException("Could not establish connection to a Zookeeper cluster " +
                    "via client '" + name + "'.");
        }
    }

    public void upsert(String path, byte[] data) {
        ensureConnected();
        try {
            if (curatorFramework.checkExists().forPath(path) == null) {
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .forPath(path, data);
            } else {
                curatorFramework.setData().forPath(path, data);
            }
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public void delete(String path) {
        ensureConnected();
        try {
            curatorFramework.delete().forPath(path);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public interface ThrowingReader<T> {
        T read(byte[] data) throws IOException;
    }
}
