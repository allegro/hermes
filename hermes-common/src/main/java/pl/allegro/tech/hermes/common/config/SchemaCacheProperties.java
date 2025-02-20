package pl.allegro.tech.hermes.common.config;

import java.time.Duration;
import pl.allegro.tech.hermes.common.schema.SchemaVersionRepositoryParameters;

public class SchemaCacheProperties implements SchemaVersionRepositoryParameters {

  private Duration refreshAfterWrite = Duration.ofMinutes(10);

  private Duration expireAfterWrite = Duration.ofHours(24);

  private Duration compiledExpireAfterAccess = Duration.ofHours(40);

  private int reloadThreadPoolSize = 2;

  private boolean enabled = true;

  private int compiledMaximumSize = 2000;

  @Override
  public boolean isCacheEnabled() {
    return enabled;
  }

  @Override
  public Duration getRefreshAfterWrite() {
    return refreshAfterWrite;
  }

  public void setRefreshAfterWrite(Duration refreshAfterWrite) {
    this.refreshAfterWrite = refreshAfterWrite;
  }

  @Override
  public Duration getExpireAfterWrite() {
    return expireAfterWrite;
  }

  public void setExpireAfterWrite(Duration expireAfterWrite) {
    this.expireAfterWrite = expireAfterWrite;
  }

  public Duration getCompiledExpireAfterAccess() {
    return compiledExpireAfterAccess;
  }

  public void setCompiledExpireAfterAccess(Duration compiledExpireAfterAccess) {
    this.compiledExpireAfterAccess = compiledExpireAfterAccess;
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
