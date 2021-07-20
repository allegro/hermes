package pl.allegro.tech.hermes.management.domain.dc;

public abstract class QueryCommand<T, K> {
    public abstract T query(DatacenterBoundRepositoryHolder<K> holder);
    public abstract Class<K> getRepositoryType();
}
