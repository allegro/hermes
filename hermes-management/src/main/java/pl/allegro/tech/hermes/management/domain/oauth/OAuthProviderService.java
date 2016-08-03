package pl.allegro.tech.hermes.management.domain.oauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;

import java.util.List;

@Component
public class OAuthProviderService {

    private final OAuthProviderRepository repository;

    private final ApiPreconditions preconditions;

    @Autowired
    public OAuthProviderService(OAuthProviderRepository repository, ApiPreconditions preconditions) {
        this.repository = repository;
        this.preconditions = preconditions;
    }

    public List<String> listOAuthProviderNames() {
        return repository.listOAuthProviderNames();
    }

    public OAuthProvider getOAuthProviderDetails(String oAuthProviderName) {
        return repository.getOAuthProviderDetails(oAuthProviderName).anonymize();
    }

    public void createOAuthProvider(OAuthProvider oAuthProvider) {
        preconditions.checkConstraints(oAuthProvider);
        repository.createOAuthProvider(oAuthProvider);
    }

    public void removeOAuthProvider(String oAuthProviderName) {
        repository.removeOAuthProvider(oAuthProviderName);
    }

    public void updateOAuthProvider(String oAuthProviderName, PatchData patch) {
        OAuthProvider retrieved = repository.getOAuthProviderDetails(oAuthProviderName);
        OAuthProvider updated = Patch.apply(retrieved, patch);
        preconditions.checkConstraints(updated);

        repository.updateOAuthProvider(updated);
    }
}
