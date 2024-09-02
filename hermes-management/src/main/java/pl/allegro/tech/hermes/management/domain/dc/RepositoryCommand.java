package pl.allegro.tech.hermes.management.domain.dc;

public abstract class RepositoryCommand<T> {

  public abstract void backup(DatacenterBoundRepositoryHolder<T> holder);

  public abstract void execute(DatacenterBoundRepositoryHolder<T> holder);

  public abstract void rollback(DatacenterBoundRepositoryHolder<T> holder, Exception exception);

  public abstract Class<T> getRepositoryType();
}
