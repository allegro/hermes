package pl.allegro.tech.hermes.management.domain.oauth.commands;

import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class CreateOAuthProviderRepositoryCommand extends RepositoryCommand<OAuthProviderRepository> {

    private final OAuthProvider provider;

    public CreateOAuthProviderRepositoryCommand(OAuthProvider provider) {
        this.provider = provider;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<OAuthProviderRepository> holder) {}

    @Override
    public void execute(DatacenterBoundRepositoryHolder<OAuthProviderRepository> holder) {
        holder.getRepository().createOAuthProvider(provider);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<OAuthProviderRepository> holder) {
        holder.getRepository().removeOAuthProvider(provider.getName());
    }

    @Override
    public Class<OAuthProviderRepository> getRepositoryType() {
        return OAuthProviderRepository.class;
    }

    @Override
    public String toString() {
        return "CreateOAuthProvider(" + provider.getName() + ")";
    }
}
