package pl.allegro.tech.hermes.management.domain.oauth;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.oauth.commands.CreateOAuthProviderRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.oauth.commands.RemoveOAuthProviderRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.oauth.commands.UpdateOAuthProviderRepositoryCommand;

@Component
public class OAuthProviderService {

  private final OAuthProviderRepository repository;
  private final ApiPreconditions preconditions;
  private final Auditor auditor;
  private final MultiDatacenterRepositoryCommandExecutor multiDcExecutor;

  @Autowired
  public OAuthProviderService(
      OAuthProviderRepository repository,
      ApiPreconditions preconditions,
      Auditor auditor,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor) {
    this.repository = repository;
    this.preconditions = preconditions;
    this.auditor = auditor;
    this.multiDcExecutor = multiDcExecutor;
  }

  public List<String> listOAuthProviderNames() {
    return repository.listOAuthProviderNames();
  }

  public OAuthProvider getOAuthProviderDetails(String oAuthProviderName) {
    return repository.getOAuthProviderDetails(oAuthProviderName).anonymize();
  }

  public void createOAuthProvider(OAuthProvider oAuthProvider, RequestUser createdBy) {
    preconditions.checkConstraints(oAuthProvider, false);
    multiDcExecutor.executeByUser(
        new CreateOAuthProviderRepositoryCommand(oAuthProvider), createdBy);
    auditor.objectCreated(createdBy.getUsername(), oAuthProvider);
  }

  public void removeOAuthProvider(String oAuthProviderName, RequestUser removedBy) {
    OAuthProvider oAuthProvider = repository.getOAuthProviderDetails(oAuthProviderName);
    multiDcExecutor.executeByUser(
        new RemoveOAuthProviderRepositoryCommand(oAuthProviderName), removedBy);
    auditor.objectRemoved(removedBy.getUsername(), oAuthProvider);
  }

  public void updateOAuthProvider(
      String oAuthProviderName, PatchData patch, RequestUser updatedBy) {
    OAuthProvider retrieved = repository.getOAuthProviderDetails(oAuthProviderName);
    OAuthProvider updated = Patch.apply(retrieved, patch);
    preconditions.checkConstraints(updated, false);

    multiDcExecutor.executeByUser(new UpdateOAuthProviderRepositoryCommand(updated), updatedBy);
    auditor.objectUpdated(updatedBy.getUsername(), retrieved, updated);
  }
}
