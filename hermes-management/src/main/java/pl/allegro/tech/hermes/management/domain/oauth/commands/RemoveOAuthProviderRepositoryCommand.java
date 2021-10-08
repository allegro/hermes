package pl.allegro.tech.hermes.management.domain.oauth.commands;

import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RemoveOAuthProviderRepositoryCommand extends RepositoryCommand<OAuthProviderRepository> {

    private final String providerName;

    private OAuthProvider backup;

    public RemoveOAuthProviderRepositoryCommand(String providerName) {
        this.providerName = providerName;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<OAuthProviderRepository> holder) {
        backup = holder.getRepository().getOAuthProviderDetails(providerName);
    }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<OAuthProviderRepository> holder) {
        holder.getRepository().removeOAuthProvider(providerName);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<OAuthProviderRepository> holder) {
        holder.getRepository().createOAuthProvider(backup);
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
