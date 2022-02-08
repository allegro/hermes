package pl.allegro.tech.hermes.management.domain.dc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;

import java.util.ArrayList;
import java.util.List;


public class MultiDatacenterRepositoryCommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MultiDatacenterRepositoryCommandExecutor.class);

    private final RepositoryManager repositoryManager;
    private final boolean rollbackEnabled;

    public MultiDatacenterRepositoryCommandExecutor(RepositoryManager repositoryManager, boolean rollbackEnabled) {
        this.repositoryManager = repositoryManager;
        this.rollbackEnabled = rollbackEnabled;
    }

    public <T> void executeByUser(RepositoryCommand<T> command, RequestUser requestUser) {
        if(requestUser.isAdmin()) execute(command, false, false);
        else execute(command);
    }

    public <T> void execute(RepositoryCommand<T> command) {
        execute(command, rollbackEnabled, true);
    }

    private <T> void execute(RepositoryCommand<T> command, boolean isRollbackEnabled, boolean shouldFailOnAnyDcFailure) {
        if (isRollbackEnabled) {
            backup(command);
        }

        List<DatacenterBoundRepositoryHolder<T>> repoHolders = repositoryManager.getRepositories(command.getRepositoryType());
        List<DatacenterBoundRepositoryHolder<T>> executedRepoHolders = new ArrayList<>();

        for (DatacenterBoundRepositoryHolder<T> repoHolder : repoHolders) {
            try {
                executedRepoHolders.add(repoHolder);
                command.execute(repoHolder);
            } catch (Exception e) {
                logger.warn("Execute failed with an error", e);
                if (isRollbackEnabled) rollback(executedRepoHolders, command);
                if (shouldFailOnAnyDcFailure)
                    throw ExceptionWrapper.wrapInInternalProcessingExceptionIfNeeded(e, command.toString(), repoHolder.getDatacenterName());
            }
        }
    }

    private <T> void rollback(List<DatacenterBoundRepositoryHolder<T>> repoHolders, RepositoryCommand<T> command) {
        for (DatacenterBoundRepositoryHolder<T> repoHolder :  repoHolders) {
            try {
                command.rollback(repoHolder);
            } catch (Exception e) {
                logger.error("Rollback procedure failed for command {} on DC {}", command, repoHolder.getDatacenterName(), e);
            }
        }
    }

    private <T> void backup(RepositoryCommand<T> command) {
        DatacenterBoundRepositoryHolder<T> repoHolder = repositoryManager.getLocalRepository(command.getRepositoryType());
        try {
            logger.debug("Creating backup for command: {}", command);
            command.backup(repoHolder);
        } catch (Exception e) {
            throw new InternalProcessingException("Backup procedure for command '" + command +
                    "' failed on DC '" + repoHolder.getDatacenterName() + "'.", e);
        }
    }
}
