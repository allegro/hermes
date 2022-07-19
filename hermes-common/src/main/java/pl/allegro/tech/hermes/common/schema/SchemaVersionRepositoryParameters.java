package pl.allegro.tech.hermes.common.schema;

public interface SchemaVersionRepositoryParameters {

    boolean isCacheEnabled();

    int getRefreshAfterWriteMinutes();

    int getExpireAfterWriteMinutes();

    int getReloadThreadPoolSize();
}
