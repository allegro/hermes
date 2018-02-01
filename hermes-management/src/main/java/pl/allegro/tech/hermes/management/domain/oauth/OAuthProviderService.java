package pl.allegro.tech.hermes.management.domain.oauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.dc.MultiDcRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.oauth.commands.CreateOAuthProviderRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.oauth.commands.RemoveOAuthProviderRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.oauth.commands.UpdateOAuthProviderRepositoryCommand;

import java.util.List;

@Component
public class OAuthProviderService {

    private final OAuthProviderRepository repository;
    private final ApiPreconditions preconditions;
    private final Auditor auditor;
    private final MultiDcRepositoryCommandExecutor multiDcExecutor;

    @Autowired
    public OAuthProviderService(OAuthProviderRepository repository, ApiPreconditions preconditions, Auditor auditor,
                                MultiDcRepositoryCommandExecutor multiDcExecutor) {
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

    public void createOAuthProvider(OAuthProvider oAuthProvider, String createdBy) {
        preconditions.checkConstraints(oAuthProvider);
        multiDcExecutor.execute(new CreateOAuthProviderRepositoryCommand(oAuthProvider));
        auditor.objectCreated(createdBy, oAuthProvider);
    }

    public void removeOAuthProvider(String oAuthProviderName, String removedBy) {
        multiDcExecutor.execute(new RemoveOAuthProviderRepositoryCommand(oAuthProviderName));
        auditor.objectRemoved(removedBy, OAuthProvider.class.getSimpleName(), oAuthProviderName);
    }

    public void updateOAuthProvider(String oAuthProviderName, PatchData patch, String updatedBy) {
        OAuthProvider retrieved = repository.getOAuthProviderDetails(oAuthProviderName);
        OAuthProvider updated = Patch.apply(retrieved, patch);
        preconditions.checkConstraints(updated);

        multiDcExecutor.execute(new UpdateOAuthProviderRepositoryCommand(updated));
        auditor.objectUpdated(updatedBy, retrieved, updated);
    }
}
