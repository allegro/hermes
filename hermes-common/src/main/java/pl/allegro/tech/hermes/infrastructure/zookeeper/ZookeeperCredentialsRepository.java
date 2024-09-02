package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.CredentialsRepository;
import pl.allegro.tech.hermes.domain.NodePassword;

public class ZookeeperCredentialsRepository extends ZookeeperBasedRepository
    implements CredentialsRepository {

  public ZookeeperCredentialsRepository(
      CuratorFramework zookeeper, ObjectMapper mapper, ZookeeperPaths paths) {
    super(zookeeper, mapper, paths);
  }

  @Override
  public NodePassword readAdminPassword() {
    return readFrom(paths.groupsPath(), NodePassword.class);
  }

  @Override
  public void overwriteAdminPassword(String password) {
    try {
      overwrite(paths.groupsPath(), new NodePassword(password));
    } catch (Exception e) {
      throw new InternalProcessingException(e);
    }
  }
}
