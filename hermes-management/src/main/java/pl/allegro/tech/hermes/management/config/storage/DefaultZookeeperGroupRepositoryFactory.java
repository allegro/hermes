package pl.allegro.tech.hermes.management.config.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class DefaultZookeeperGroupRepositoryFactory implements ZookeeperGroupRepositoryFactory {
  @Override
  public GroupRepository create(
      CuratorFramework zookeeper, ObjectMapper mapper, ZookeeperPaths paths) {
    return new ZookeeperGroupRepository(zookeeper, mapper, paths);
  }
}
