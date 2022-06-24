package pl.allegro.tech.hermes.common.schema;

public class SchemaVersionRepositoryParameters {

    private final boolean cacheEnabled;

    private final int refreshAfterWriteMinutes;

    private final int expireAfterWriteMinutes;

    private final int reloadThreadPoolSize;

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public int getRefreshAfterWriteMinutes() {
        return refreshAfterWriteMinutes;
    }

    public int getExpireAfterWriteMinutes() {
        return expireAfterWriteMinutes;
    }

    public int getReloadThreadPoolSize() {
        return reloadThreadPoolSize;
    }

    public SchemaVersionRepositoryParameters(boolean cacheEnabled, int refreshAfterWriteMinutes,
                                             int expireAfterWriteMinutes, int reloadThreadPoolSize) {
        this.cacheEnabled = cacheEnabled;
        this.refreshAfterWriteMinutes = refreshAfterWriteMinutes;
        this.expireAfterWriteMinutes = expireAfterWriteMinutes;
        this.reloadThreadPoolSize = reloadThreadPoolSize;
    }
}
