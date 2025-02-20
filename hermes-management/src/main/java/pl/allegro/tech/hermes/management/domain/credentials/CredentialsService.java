package pl.allegro.tech.hermes.management.domain.credentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.domain.CredentialsRepository;
import pl.allegro.tech.hermes.domain.NodePassword;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.credentials.commands.UpdateCredentialsRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;

@Component
public class CredentialsService {

  private static final Logger logger = LoggerFactory.getLogger(CredentialsService.class);

  private final ZookeeperPaths paths;
  private final MultiDatacenterRepositoryCommandExecutor multiDcExecutor;
  private final CredentialsRepository credentialsRepository;

  @Autowired
  public CredentialsService(
      ZookeeperPaths paths,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor,
      CredentialsRepository credentialsRepository) {
    this.paths = paths;
    this.multiDcExecutor = multiDcExecutor;
    this.credentialsRepository = credentialsRepository;
  }

  public NodePassword readAdminPassword() {
    return credentialsRepository.readAdminPassword();
  }

  public void overwriteAdminPassword(String password) {
    multiDcExecutor.execute(
        new UpdateCredentialsRepositoryCommand(credentialsRepository, password));
  }
}
