package pl.allegro.tech.hermes.management.domain.dc;

public abstract class RepositoryCommand<T> {

    public abstract void backup(T repository);

    public abstract void execute(T repository);

    public abstract void rollback(T repository);

    public abstract Class<T> getRepositoryType();

}
