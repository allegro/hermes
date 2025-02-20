package pl.allegro.tech.hermes.management.domain.dc;

import java.util.List;

public interface RepositoryManager {

  <T> DatacenterBoundRepositoryHolder<T> getLocalRepository(Class<T> repositoryType);

  <T> List<DatacenterBoundRepositoryHolder<T>> getRepositories(Class<T> repositoryType);
}
