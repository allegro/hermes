package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.ArrayUtils;
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
import java.util.function.BiConsumer;

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
    protected <T> Optional<T> readFrom(String path, TypeReference<T> type, boolean quiet) {
        return readFrom(path, b -> (T) mapper.readValue(b, type), quiet);
    }

    protected <T> Optional<T> readWithStatFrom(String path, Class<T> clazz, BiConsumer<T, Stat> statDecorator, boolean quiet) {
        return readWithStatFrom(path, b -> mapper.readValue(b, clazz), statDecorator, quiet);
    }

    private <T> Optional<T> readFrom(String path, ThrowingReader<T> supplier, boolean quiet) {
        try {
            byte[] data = zookeeper.getData().forPath(path);
            if (ArrayUtils.isNotEmpty(data)) {
                return Optional.of(supplier.read(data));
            } else {
                logWarnOrThrowException("No data at path " + path,
                        new InternalProcessingException("No data at path " + path),
                        quiet);
            }
        } catch (JsonMappingException malformedException) {
            logWarnOrThrowException("Unable to read data from path " + path,
                    new MalformedDataException(path, malformedException), quiet);
        } catch (InternalProcessingException e) {
            throw e;
        } catch (Exception exception) {
            logWarnOrThrowException("Unable to read data from path " + path, new InternalProcessingException(exception),
                    quiet);
        }
        return Optional.empty();
    }

    private <T> Optional<T> readWithStatFrom(String path, ThrowingReader<T> supplier, BiConsumer<T, Stat> statDecorator, boolean quiet) {
        try {
            Stat stat = new Stat();
            byte[] data = zookeeper.getData().storingStatIn(stat).forPath(path);
            if (ArrayUtils.isNotEmpty(data)) {
                T t = supplier.read(data);
                statDecorator.accept(t, stat);
                return Optional.of(t);
            } else {
                logWarnOrThrowException("No data at path " + path,
                        new InternalProcessingException("No data at path " + path),
                        quiet);
            }
        } catch (JsonMappingException malformedException) {
            logWarnOrThrowException("Unable to read data from path " + path,
                    new MalformedDataException(path, malformedException), quiet);
        } catch (InternalProcessingException e) {
            throw e;
        } catch (Exception exception) {
            logWarnOrThrowException("Unable to read data from path " + path, new InternalProcessingException(exception),
                    quiet);
        }
        return Optional.empty();
    }

    private void logWarnOrThrowException(String message, RuntimeException e, Boolean quiet) {
        if (quiet) {
            logger.warn(message, e);
        } else {
            throw e;
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

    protected boolean isEmpty(String path) {
        try {
            byte[] data = zookeeper.getData().forPath(path);
            return data.length == 0;
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private interface ThrowingReader<T> {
        T read(byte[] data) throws IOException;
    }
}
