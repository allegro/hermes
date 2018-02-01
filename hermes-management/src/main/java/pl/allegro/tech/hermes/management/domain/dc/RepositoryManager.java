package pl.allegro.tech.hermes.management.domain.dc;

import java.util.List;

public interface RepositoryManager {

    <T> DcBoundRepositoryHolder<T> getLocalRepository(Class<T> repositoryType);

    <T> List<DcBoundRepositoryHolder<T>> getRepositories(Class<T> repositoryType);
}
