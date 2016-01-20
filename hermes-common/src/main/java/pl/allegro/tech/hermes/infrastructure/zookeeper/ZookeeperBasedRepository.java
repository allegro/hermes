package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.MalformedDataException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class ZookeeperBasedRepository {

    protected final CuratorFramework zookeeper;

    protected final ObjectMapper mapper;

    protected final ZookeeperPaths paths;

    protected ZookeeperBasedRepository(CuratorFramework zookeeper,
                                       ObjectMapper mapper,
                                       ZookeeperPaths paths) {
        this.zookeeper = zookeeper;
        this.mapper = mapper;
        this.paths = paths;
    }

    protected void ensureConnected() {
        if (!zookeeper.getZookeeperClient().isConnected()) {
            throw new InternalProcessingException("Could not establish connection to a Zookeeper instance");
        }
    }

    protected boolean pathExists(String path) {
        ensureConnected();
        try {
            Optional<Stat> optionalStat = Optional.ofNullable(zookeeper.checkExists().forPath(path));
            return optionalStat.isPresent();
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    protected List<String> childrenOf(String path) {
        try {
            List<String> retrievedNodes = new ArrayList<>(zookeeper.getChildren().forPath(path));
            Collections.sort(retrievedNodes);
            return retrievedNodes;
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    protected <T> T readFrom(String path, Class<T> clazz) {
        try {
            return mapper.readValue(zookeeper.getData().forPath(path), clazz);
        } catch (JsonMappingException exception) {
            throw new MalformedDataException(path, exception);
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T readFrom(String path, Class<T> clazz, PostProcessor postProcessor) {
        try {
            byte[] data = zookeeper.getData().forPath(path);
            return (T) postProcessor.invoke(data, mapper.readValue(data, clazz));
        } catch (JsonMappingException exception) {
            throw new MalformedDataException(path, exception);
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    protected void overwrite(String path, Object value) {
        try {
            zookeeper.setData().forPath(path, mapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    protected void touch(String path) {
        try {
            byte[] oldData = zookeeper.getData().forPath(path);
            zookeeper.setData().forPath(path, oldData);
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    protected void remove(String path) {
        try {
            zookeeper.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    interface PostProcessor {
         Object invoke(byte[] data, Object value);
    }
}
