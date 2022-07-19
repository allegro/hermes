package pl.allegro.tech.hermes.common.config;

import pl.allegro.tech.hermes.common.schema.SchemaVersionRepositoryParameters;

public class SchemaCacheProperties implements SchemaVersionRepositoryParameters {

    private int refreshAfterWriteMinutes = 10;

    private int expireAfterWriteMinutes = 60 * 24;

    private int compiledExpireAfterAccessMinutes = 60 * 40;

    private int reloadThreadPoolSize = 2;

    private boolean enabled = true;

    private int compiledMaximumSize = 2000;

    @Override
    public boolean isCacheEnabled() {
        return enabled;
    }

    @Override
    public int getRefreshAfterWriteMinutes() {
        return refreshAfterWriteMinutes;
    }

    public void setRefreshAfterWriteMinutes(int refreshAfterWriteMinutes) {
        this.refreshAfterWriteMinutes = refreshAfterWriteMinutes;
    }

    @Override
    public int getExpireAfterWriteMinutes() {
        return expireAfterWriteMinutes;
    }

    public void setExpireAfterWriteMinutes(int expireAfterWriteMinutes) {
        this.expireAfterWriteMinutes = expireAfterWriteMinutes;
    }

    public int getCompiledExpireAfterAccessMinutes() {
        return compiledExpireAfterAccessMinutes;
    }

    public void setCompiledExpireAfterAccessMinutes(int compiledExpireAfterAccessMinutes) {
        this.compiledExpireAfterAccessMinutes = compiledExpireAfterAccessMinutes;
    }

    @Override
    public int getReloadThreadPoolSize() {
        return reloadThreadPoolSize;
    }

    public void setReloadThreadPoolSize(int reloadThreadPoolSize) {
        this.reloadThreadPoolSize = reloadThreadPoolSize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCompiledMaximumSize() {
        return compiledMaximumSize;
    }

    public void setCompiledMaximumSize(int compiledMaximumSize) {
        this.compiledMaximumSize = compiledMaximumSize;
    }
}
