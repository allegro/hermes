package pl.allegro.tech.hermes.management.domain.oauth.commands;

import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class UpdateOAuthProviderRepositoryCommand extends RepositoryCommand<OAuthProviderRepository> {

    private final OAuthProvider provider;

    private OAuthProvider backup;

    public UpdateOAuthProviderRepositoryCommand(OAuthProvider provider) {
        this.provider = provider;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<OAuthProviderRepository> holder) {
        backup = holder.getRepository().getOAuthProviderDetails(provider.getName());
    }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<OAuthProviderRepository> holder) {
        holder.getRepository().updateOAuthProvider(provider);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<OAuthProviderRepository> holder) {
        holder.getRepository().updateOAuthProvider(backup);
    }

    @Override
    public Class<OAuthProviderRepository> getRepositoryType() {
        return OAuthProviderRepository.class;
    }

    @Override
    public String toString() {
        return "UpdateOAuthProvider(" + provider.getName() + ")";
    }
}
