package pl.allegro.tech.hermes.common.schema;

import java.time.Duration;

public interface SchemaVersionRepositoryParameters {

  boolean isCacheEnabled();

  Duration getRefreshAfterWrite();

  Duration getExpireAfterWrite();

  int getReloadThreadPoolSize();
}
