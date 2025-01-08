package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.config.SchemaCacheProperties;
import pl.allegro.tech.hermes.common.config.SchemaRepositoryProperties;

@ConfigurationProperties(prefix = "consumer.schema")
public class SchemaProperties {

  private SchemaCacheProperties cache = new SchemaCacheProperties();

  private SchemaRepositoryProperties repository = new SchemaRepositoryProperties();

  private boolean idHeaderEnabled = false;

  private boolean idSerializationEnabled = false;

  private boolean versionTruncationEnabled = false;

  public SchemaCacheProperties getCache() {
    return cache;
  }

  public void setCache(SchemaCacheProperties cache) {
    this.cache = cache;
  }

  public SchemaRepositoryProperties getRepository() {
    return repository;
  }

  public void setRepository(SchemaRepositoryProperties repository) {
    this.repository = repository;
  }

  public boolean isIdHeaderEnabled() {
    return idHeaderEnabled;
  }

  public void setIdHeaderEnabled(boolean idHeaderEnabled) {
    this.idHeaderEnabled = idHeaderEnabled;
  }

  public boolean isIdSerializationEnabled() {
    return idSerializationEnabled;
  }

  public void setIdSerializationEnabled(boolean idSerializationEnabled) {
    this.idSerializationEnabled = idSerializationEnabled;
  }

  public boolean isVersionTruncationEnabled() {
    return versionTruncationEnabled;
  }

  public void setVersionTruncationEnabled(boolean versionTruncationEnabled) {
    this.versionTruncationEnabled = versionTruncationEnabled;
  }
}
