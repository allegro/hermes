package pl.allegro.tech.hermes.management.domain.credentials.commands;

import pl.allegro.tech.hermes.domain.CredentialsRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class UpdateCredentialsRepositoryCommand extends RepositoryCommand<CredentialsRepository> {

    private final CredentialsRepository repository;
    private final String password;

    public UpdateCredentialsRepositoryCommand(CredentialsRepository repository, String password) {
        this.repository = repository;
        this.password = password;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<CredentialsRepository> holder) {
    }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<CredentialsRepository> holder) {
        repository.overwriteAdminPassword(password);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<CredentialsRepository> holder) {
    }

    @Override
    public Class<CredentialsRepository> getRepositoryType() {
        return CredentialsRepository.class;
    }
}
