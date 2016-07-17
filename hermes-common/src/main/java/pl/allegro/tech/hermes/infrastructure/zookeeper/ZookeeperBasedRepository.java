package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.MalformedDataException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class ZookeeperBasedRepository {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperBasedRepository.class);

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

    protected void ensurePathExists(String path) {
        ensureConnected();
        if (!pathExists(path)) {
            try {
                zookeeper.create().creatingParentsIfNeeded().forPath(path);
            } catch (Exception e) {
                throw new InternalProcessingException(e);
            }
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

    @SuppressWarnings("unchecked")
    protected <T> T readFrom(String path, Class<T> clazz) {
        return readFrom(path, clazz, false).get();
    }

    @SuppressWarnings("unchecked")
    protected <T> Optional<T> readFrom(String path, Class<T> clazz, boolean quiet) {
        return readFrom(path, b -> (T) mapper.readValue(b, clazz), quiet);
    }

    @SuppressWarnings("unchecked")
    protected <T> T readFrom(String path, TypeReference<T> type) {
        return readFrom(path, b -> (T) mapper.readValue(b, type), false).get();
    }

    private <T> Optional<T> readFrom(String path, ThrowingReader<T> supplier, boolean quiet) {
        try {
            byte[] data = zookeeper.getData().forPath(path);
            return Optional.of(supplier.read(data));
        } catch (JsonMappingException malformedException) {
            if (quiet) {
                logger.warn("Unable to read data from path {}", path, malformedException);
            } else {
                throw new MalformedDataException(path, malformedException);
            }
        } catch (Exception exception) {
            if (quiet) {
                logger.warn("Unable to read data from path {}", path, exception);
            } else {
                throw new InternalProcessingException(exception);
            }
        }
        return Optional.empty();
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

    private interface ThrowingReader<T> {
        T read(byte[] data) throws IOException;
    }
}
