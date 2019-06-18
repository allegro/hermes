package pl.allegro.tech.hermes.management.domain.dc;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.HermesException;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import java.util.ArrayList;
import java.util.List;


public class MultiDcRepositoryCommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MultiDcRepositoryCommandExecutor.class);

    private final RepositoryManager repositoryManager;
    private final boolean rollbackEnabled;

    public MultiDcRepositoryCommandExecutor(RepositoryManager repositoryManager, boolean rollbackEnabled) {
        this.repositoryManager = repositoryManager;
        this.rollbackEnabled = rollbackEnabled;
    }

    public <T> void execute(RepositoryCommand<T> command) {
        if (rollbackEnabled) {
            backup(command);
        }

        List<DcBoundRepositoryHolder<T>> repositories = repositoryManager.getRepositories(command.getRepositoryType());
        List<DcBoundRepositoryHolder<T>> executedRepositories = new ArrayList<>();

        for (DcBoundRepositoryHolder<T> repository : repositories) {
            try {
                executedRepositories.add(repository);
                command.execute(repository.getRepository());
            } catch (Exception e) {
                logger.warn("Execute failed with an error", e);
                if (rollbackEnabled) {
                    rollback(executedRepositories, command);
                }
                throw wrapInInternalProcessingExceptionIfNeeded(e, command.toString(), repository.getDcName());
            }
        }
    }

    private RuntimeException wrapInInternalProcessingExceptionIfNeeded(Exception toWrap,
                                                                       String command,
                                                                       String dcName) {
        if (toWrap instanceof HermesException) {
            return (HermesException) toWrap;
        }
        return new InternalProcessingException("Execution of command '" + command + "' failed on DC '" +
                dcName + "'.", toWrap);
    }

    private <T> void rollback(List<DcBoundRepositoryHolder<T>> repositories, RepositoryCommand<T> command) {
        for (DcBoundRepositoryHolder<T> repository :  Lists.reverse(repositories)) {
            try {
                command.rollback(repository.getRepository());
            } catch (Exception e) {
                logger.error("Rollback procedure failed for command {} on DC {}", command, repository.getDcName(), e);
            }
        }
    }

    private <T> void backup(RepositoryCommand<T> command) {
        DcBoundRepositoryHolder<T> repository = repositoryManager.getLocalRepository(command.getRepositoryType());
        try {
            logger.debug("Creating backup for command: {}", command);
            command.backup(repository.getRepository());
        } catch (Exception e) {
            throw new InternalProcessingException("Backup procedure for command '" + command +
                    "' failed on DC '" + repository.getDcName() + "'.", e);
        }
    }
}
