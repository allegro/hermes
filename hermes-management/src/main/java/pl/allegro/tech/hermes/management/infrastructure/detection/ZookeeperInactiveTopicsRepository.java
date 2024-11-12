package pl.allegro.tech.hermes.management.infrastructure.detection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperBasedRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopic;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsRepository;

public class ZookeeperInactiveTopicsRepository extends ZookeeperBasedRepository
    implements InactiveTopicsRepository {

  private static final Logger logger =
      LoggerFactory.getLogger(ZookeeperInactiveTopicsRepository.class);

  public ZookeeperInactiveTopicsRepository(
      CuratorFramework curatorFramework, ObjectMapper objectMapper, ZookeeperPaths paths) {
    super(curatorFramework, objectMapper, paths);
  }

  @Override
  public void upsert(List<InactiveTopic> inactiveTopics) {
    logger.info("Saving inactive topics metadata into zookeeper, count={}", inactiveTopics.size());
    String path = paths.inactiveTopicsPath();
    try {
      if (pathExists(path)) {
        overwrite(path, inactiveTopics);
      } else {
        createRecursively(path, inactiveTopics);
      }
    } catch (Exception e) {
      throw new InternalProcessingException(e);
    }
  }

  @Override
  public List<InactiveTopic> read() {
    String path = paths.inactiveTopicsPath();
    if (!pathExists(path)) {
      logger.warn("Inactive topics ZK node does not exist: {}", path);
      return Collections.emptyList();
    }
    return readFrom(paths.inactiveTopicsPath(), new TypeReference<List<InactiveTopic>>() {}, true)
        .orElse(Collections.emptyList());
  }
}
