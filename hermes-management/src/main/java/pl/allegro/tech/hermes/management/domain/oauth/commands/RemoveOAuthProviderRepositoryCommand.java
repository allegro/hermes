package pl.allegro.tech.hermes.management.domain.oauth.commands;

import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RemoveOAuthProviderRepositoryCommand extends RepositoryCommand<OAuthProviderRepository> {

    private final String providerName;

    private OAuthProvider backup;

    public RemoveOAuthProviderRepositoryCommand(String providerName) {
        this.providerName = providerName;
    }

    @Override
    public void backup(OAuthProviderRepository repository) {
        backup = repository.getOAuthProviderDetails(providerName);
    }

    @Override
    public void execute(OAuthProviderRepository repository) {
        repository.removeOAuthProvider(providerName);
    }

    @Override
    public void rollback(OAuthProviderRepository repository) {
        repository.createOAuthProvider(backup);
    }

    @Override
    public Class<OAuthProviderRepository> getRepositoryType() {
        return OAuthProviderRepository.class;
    }

    @Override
    public String toString() {
        return "RemoveOAuthProvider(" + providerName + ")";
    }
}
