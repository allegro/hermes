package pl.allegro.tech.hermes.management.domain.dc;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MultiDatacenterRepositoryQueryExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MultiDatacenterRepositoryQueryExecutor.class);

    private final RepositoryManager repositoryManager;

    public MultiDatacenterRepositoryQueryExecutor(RepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
    }

    public <T, K> List<DatacenterBoundQueryResult<T>> execute(QueryCommand<T, K> command) {

        List<DatacenterBoundRepositoryHolder<K>> repoHolders = repositoryManager.getRepositories(command.getRepositoryType());

        return repoHolders.stream().map(holder -> {
            try {
                return new DatacenterBoundQueryResult<>(command.query(holder), holder.getDatacenterName());
            } catch (Exception e) {
                logger.warn("Execute failed with an error", e);
                throw ExceptionWrapper.wrapInInternalProcessingExceptionIfNeeded(e, command.toString(), holder.getDatacenterName());
            }
        }).collect(Collectors.toList());
    }
}
