package pl.allegro.tech.hermes.management.domain.credentials.commands;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.CredentialsRepository;
import pl.allegro.tech.hermes.domain.NodePassword;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class UpdateCredentialsRepositoryCommand extends RepositoryCommand<CredentialsRepository> {

    private final CredentialsRepository repository;
    private final String password;

    public UpdateCredentialsRepositoryCommand(CredentialsRepository repository, String password) {
        this.repository = repository;
        this.password = password;
    }

    @Override
    public void backup(CredentialsRepository repository) {
    }

    @Override
    public void execute(CredentialsRepository repository) {
        repository.overwriteAdminPassword(password);
    }

    @Override
    public void rollback(CredentialsRepository repository) {
    }

    @Override
    public Class<CredentialsRepository> getRepositoryType() {
        return CredentialsRepository.class;
    }
}
