package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorOp;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.exception.RepositoryNotAvailableException;
import pl.allegro.tech.hermes.infrastructure.MalformedDataException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public abstract class ZookeeperBasedRepository {

  private static final Logger logger = LoggerFactory.getLogger(ZookeeperBasedRepository.class);

  private final CuratorFramework zookeeper;

  private final ObjectMapper mapper;

  protected final ZookeeperPaths paths;

  protected ZookeeperBasedRepository(
      CuratorFramework zookeeper, ObjectMapper mapper, ZookeeperPaths paths) {
    this.zookeeper = zookeeper;
    this.mapper = mapper;
    this.paths = paths;
  }

  private void ensureConnected() {
    if (!zookeeper.getZookeeperClient().isConnected()) {
      throw new RepositoryNotAvailableException(
          "Could not establish connection to a Zookeeper instance");
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
    ensureConnected();
    try {
      List<String> retrievedNodes = new ArrayList<>(zookeeper.getChildren().forPath(path));
      Collections.sort(retrievedNodes);
      return retrievedNodes;
    } catch (Exception ex) {
      throw new InternalProcessingException(ex);
    }
  }

  protected List<String> childrenPathsOf(String path) {
    List<String> childNodes = childrenOf(path);
    return childNodes.stream()
        .map(child -> ZKPaths.makePath(path, child))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  protected byte[] readFrom(String path) {
    return readWithStatFrom(path, bytes -> bytes, (t, stat) -> {}, false).get();
  }

  @SuppressWarnings("unchecked")
  protected <T> T readFrom(String path, Class<T> clazz) {
    return readFrom(path, clazz, false).get();
  }

  @SuppressWarnings("unchecked")
  protected <T> Optional<T> readFrom(String path, Class<T> clazz, boolean quiet) {
    return readWithStatFrom(path, b -> (T) mapper.readValue(b, clazz), (t, stat) -> {}, quiet);
  }

  @SuppressWarnings("unchecked")
  protected <T> Optional<T> readFrom(String path, TypeReference<T> type, boolean quiet) {
    return readWithStatFrom(path, b -> (T) mapper.readValue(b, type), (t, stat) -> {}, quiet);
  }

  protected <T> Optional<T> readWithStatFrom(
      String path, Class<T> clazz, BiConsumer<T, Stat> statDecorator, boolean quiet) {
    return readWithStatFrom(path, b -> mapper.readValue(b, clazz), statDecorator, quiet);
  }

  private <T> Optional<T> readWithStatFrom(
      String path, ThrowingReader<T> supplier, BiConsumer<T, Stat> statDecorator, boolean quiet) {
    ensureConnected();
    try {
      Stat stat = new Stat();
      byte[] data = zookeeper.getData().storingStatIn(stat).forPath(path);
      if (ArrayUtils.isNotEmpty(data)) {
        T t = supplier.read(data);
        statDecorator.accept(t, stat);
        return Optional.of(t);
      }
    } catch (JsonMappingException malformedException) {
      logWarnOrThrowException(
          "Unable to read data from path " + path,
          new MalformedDataException(path, malformedException),
          quiet);
    } catch (InternalProcessingException e) {
      throw e;
    } catch (Exception exception) {
      logWarnOrThrowException(
          "Unable to read data from path " + path,
          new InternalProcessingException(exception),
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

  protected void overwrite(String path, Object value) throws Exception {
    ensureConnected();
    zookeeper.setData().forPath(path, mapper.writeValueAsBytes(value));
  }

  protected void overwrite(String path, byte[] value) throws Exception {
    ensureConnected();
    zookeeper.setData().forPath(path, value);
  }

  protected void createRecursively(String path, Object value) throws Exception {
    ensureConnected();
    zookeeper.create().creatingParentsIfNeeded().forPath(path, mapper.writeValueAsBytes(value));
  }

  protected void createInTransaction(String path, Object value, String childPath) throws Exception {
    ensureConnected();
    zookeeper.transaction().forOperations(
            zookeeper.transactionOp().create().forPath(path, mapper.writeValueAsBytes(value)),
            zookeeper.transactionOp().create().forPath(childPath)
    );
  }

  protected void deleteInTransaction(List<String> paths) throws Exception {
    if (paths.isEmpty()) {
      throw new InternalProcessingException("Attempting to remove empty set of paths from ZK");
    }
    ensureConnected();
    List<CuratorOp> operations = new ArrayList<>(paths.size());
    for (String path : paths) {
      operations.add(zookeeper.transactionOp().delete().forPath(path));
    }
    zookeeper.transaction().forOperations(operations);
  }

  protected void create(String path, Object value) throws Exception {
    ensureConnected();
    zookeeper.create().forPath(path, mapper.writeValueAsBytes(value));
  }

  protected void create(String path, byte[] value) throws Exception {
    ensureConnected();
    zookeeper.create().forPath(path, value);
  }

  protected void touch(String path) throws Exception {
    ensureConnected();
    byte[] oldData = zookeeper.getData().forPath(path);
    zookeeper.setData().forPath(path, oldData);
  }

  protected void remove(String path) throws Exception {
    ensureConnected();
    zookeeper.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
  }

  private interface ThrowingReader<T> {
    T read(byte[] data) throws IOException;
  }
}
