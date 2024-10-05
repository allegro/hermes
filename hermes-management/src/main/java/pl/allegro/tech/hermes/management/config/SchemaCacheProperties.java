package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "schema.cache")
public class SchemaCacheProperties {

  private int poolSize = 2;
  private int refreshAfterWriteMinutes = 10;
  private int expireAfterWriteMinutes = 60 * 24;

  public int getPoolSize() {
    return poolSize;
  }

  public void setPoolSize(int poolSize) {
    this.poolSize = poolSize;
  }

  public int getRefreshAfterWriteMinutes() {
    return refreshAfterWriteMinutes;
  }

  public void setRefreshAfterWriteMinutes(int refreshAfterWriteMinutes) {
    this.refreshAfterWriteMinutes = refreshAfterWriteMinutes;
  }

  public int getExpireAfterWriteMinutes() {
    return expireAfterWriteMinutes;
  }

  public void setExpireAfterWriteMinutes(int expireAfterWriteMinutes) {
    this.expireAfterWriteMinutes = expireAfterWriteMinutes;
  }
}
