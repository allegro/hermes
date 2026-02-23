package pl.allegro.tech.hermes.management.domain.credentials;

import pl.allegro.tech.hermes.domain.CredentialsRepository;
import pl.allegro.tech.hermes.domain.NodePassword;
import pl.allegro.tech.hermes.management.domain.credentials.commands.UpdateCredentialsRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;

public class CredentialsService {

  private final MultiDatacenterRepositoryCommandExecutor multiDcExecutor;
  private final CredentialsRepository credentialsRepository;

  public CredentialsService(
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor,
      CredentialsRepository credentialsRepository) {
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
