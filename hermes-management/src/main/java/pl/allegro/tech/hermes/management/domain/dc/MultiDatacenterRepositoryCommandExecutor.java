package pl.allegro.tech.hermes.management.domain.dc;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.exception.RepositoryNotAvailableException;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;

public class MultiDatacenterRepositoryCommandExecutor {

  private static final Logger logger =
      LoggerFactory.getLogger(MultiDatacenterRepositoryCommandExecutor.class);

  private final RepositoryManager repositoryManager;
  private final boolean rollbackEnabled;
  private final ModeService modeService;

  public MultiDatacenterRepositoryCommandExecutor(
      RepositoryManager repositoryManager, boolean rollbackEnabled, ModeService modeService) {
    this.repositoryManager = repositoryManager;
    this.rollbackEnabled = rollbackEnabled;
    this.modeService = modeService;
  }

  public <T> void executeByUser(RepositoryCommand<T> command, RequestUser requestUser) {
    if (requestUser.isAdmin() && modeService.isReadOnlyEnabled()) {
      execute(command, false, false);
    } else {
      execute(command);
    }
  }

  public <T> void execute(RepositoryCommand<T> command) {
    execute(command, rollbackEnabled, true);
  }

  private <T> void execute(
      RepositoryCommand<T> command,
      boolean isRollbackEnabled,
      boolean shouldStopExecutionOnFailure) {
    if (isRollbackEnabled) {
      backup(command);
    }

    List<DatacenterBoundRepositoryHolder<T>> repoHolders =
        repositoryManager.getRepositories(command.getRepositoryType());
    List<DatacenterBoundRepositoryHolder<T>> executedRepoHolders = new ArrayList<>();

    for (DatacenterBoundRepositoryHolder<T> repoHolder : repoHolders) {
      long start = System.currentTimeMillis();
      try {
        executedRepoHolders.add(repoHolder);
        logger.info(
            "Executing repository command: {} in ZK dc: {}",
            command,
            repoHolder.getDatacenterName());
        command.execute(repoHolder);
        logger.info(
            "Successfully executed repository command: {} in ZK dc: {} in: {} ms",
            command,
            repoHolder.getDatacenterName(),
            System.currentTimeMillis() - start);
      } catch (RepositoryNotAvailableException e) {
        logger.warn("Execute failed with an RepositoryNotAvailableException error", e);
        if (isRollbackEnabled) {
          rollback(executedRepoHolders, command, e);
        }
        if (shouldStopExecutionOnFailure) {
          throw ExceptionWrapper.wrapInInternalProcessingExceptionIfNeeded(
              e, command.toString(), repoHolder.getDatacenterName());
        }
      } catch (Exception e) {
        logger.warn(
            "Failed to execute repository command: {} in ZK dc: {} in: {} ms",
            command,
            repoHolder.getDatacenterName(),
            System.currentTimeMillis() - start,
            e);
        if (isRollbackEnabled) {
          rollback(executedRepoHolders, command, e);
        }
        throw ExceptionWrapper.wrapInInternalProcessingExceptionIfNeeded(
            e, command.toString(), repoHolder.getDatacenterName());
      }
    }
  }

  private <T> void rollback(
      List<DatacenterBoundRepositoryHolder<T>> repoHolders,
      RepositoryCommand<T> command,
      Exception exception) {
    long start = System.currentTimeMillis();
    for (DatacenterBoundRepositoryHolder<T> repoHolder : repoHolders) {
      logger.info(
          "Executing rollback of repository command: {} in ZK dc: {}",
          command,
          repoHolder.getDatacenterName());
      try {
        command.rollback(repoHolder, exception);
        logger.info(
            "Successfully executed rollback of repository command: {} in ZK dc: {} in: {} ms",
            command,
            repoHolder.getDatacenterName(),
            System.currentTimeMillis() - start);
      } catch (Exception e) {
        logger.error(
            "Rollback procedure failed for command {} on DC {}",
            command,
            repoHolder.getDatacenterName(),
            e);
      }
    }
  }

  private <T> void backup(RepositoryCommand<T> command) {
    DatacenterBoundRepositoryHolder<T> repoHolder =
        repositoryManager.getLocalRepository(command.getRepositoryType());
    try {
      logger.debug("Creating backup for command: {}", command);
      command.backup(repoHolder);
    } catch (Exception e) {
      throw new InternalProcessingException(
          "Backup procedure for command '"
              + command
              + "' failed on DC '"
              + repoHolder.getDatacenterName()
              + "'.",
          e);
    }
  }
}
