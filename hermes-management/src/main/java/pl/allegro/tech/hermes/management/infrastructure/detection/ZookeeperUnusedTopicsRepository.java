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
import pl.allegro.tech.hermes.management.domain.detection.UnusedTopic;
import pl.allegro.tech.hermes.management.domain.detection.UnusedTopicsRepository;

public class ZookeeperUnusedTopicsRepository extends ZookeeperBasedRepository
    implements UnusedTopicsRepository {

  private static final Logger logger =
      LoggerFactory.getLogger(ZookeeperUnusedTopicsRepository.class);

  public ZookeeperUnusedTopicsRepository(
      CuratorFramework curatorFramework, ObjectMapper objectMapper, ZookeeperPaths paths) {
    super(curatorFramework, objectMapper, paths);
  }

  @Override
  public void upsert(List<UnusedTopic> unusedTopics) {
    logger.info(
        "Saving unused topics metadata into zookeeper, number of unused topics: {}",
        unusedTopics.size());
    String path = paths.unusedTopicsPath();
    try {
      if (pathExists(path)) {
        overwrite(path, unusedTopics);
      } else {
        createRecursively(path, unusedTopics);
      }
    } catch (Exception e) {
      throw new InternalProcessingException(e);
    }
  }

  @Override
  public List<UnusedTopic> read() {
    String path = paths.unusedTopicsPath();
    if (!pathExists(path)) {
      logger.warn("Unused topics ZK node does not exist: {}", path);
      return Collections.emptyList();
    }
    return readFrom(paths.unusedTopicsPath(), new TypeReference<List<UnusedTopic>>() {}, true)
        .orElse(Collections.emptyList());
  }
}
