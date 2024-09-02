package pl.allegro.tech.hermes.management.infrastructure.blacklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperBasedRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.blacklist.NotUnblacklistedException;
import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistRepository;

public class ZookeeperTopicBlacklistRepository extends ZookeeperBasedRepository
    implements TopicBlacklistRepository {

  private static final Logger logger =
      LoggerFactory.getLogger(ZookeeperTopicBlacklistRepository.class);

  public ZookeeperTopicBlacklistRepository(
      CuratorFramework zookeeper, ObjectMapper mapper, ZookeeperPaths paths) {
    super(zookeeper, mapper, paths);
  }

  @Override
  public void add(String qualifiedTopicName) {
    logger.info("Adding topic {} to Blacklist", qualifiedTopicName);
    ensurePathExists(paths.blacklistedTopicPath(qualifiedTopicName));
  }

  @Override
  public void remove(String qualifiedTopicName) {
    logger.info("Removing topic {} from Blacklist", qualifiedTopicName);
    try {
      super.remove(paths.blacklistedTopicPath(qualifiedTopicName));
    } catch (Exception e) {
      logger.warn("Removing topic {} from Blacklist caused an exception", qualifiedTopicName, e);
      throw new NotUnblacklistedException(qualifiedTopicName, e);
    }
  }

  @Override
  public boolean isBlacklisted(String qualifiedTopicName) {
    return pathExists(paths.blacklistedTopicPath(qualifiedTopicName));
  }

  @Override
  public List<String> list() {
    return childrenOf(paths.topicsBlacklistPath());
  }
}
