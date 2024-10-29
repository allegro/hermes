package pl.allegro.tech.hermes.management.config.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public interface ZookeeperGroupRepositoryFactory {
  GroupRepository create(CuratorFramework zookeeper, ObjectMapper mapper, ZookeeperPaths paths);
}
