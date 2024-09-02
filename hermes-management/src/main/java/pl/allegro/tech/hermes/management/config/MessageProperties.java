package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "message")
public class MessageProperties {

  private String contentRoot = "message";

  private String metadataContentRoot = "metadata";

  private boolean schemaIdHeaderEnabled = false;

  private boolean schemaVersionTruncationEnabled = false;

  public String getContentRoot() {
    return contentRoot;
  }

  public void setContentRoot(String contentRoot) {
    this.contentRoot = contentRoot;
  }

  public String getMetadataContentRoot() {
    return metadataContentRoot;
  }

  public void setMetadataContentRoot(String metadataContentRoot) {
    this.metadataContentRoot = metadataContentRoot;
  }

  public boolean isSchemaIdHeaderEnabled() {
    return schemaIdHeaderEnabled;
  }

  public void setSchemaIdHeaderEnabled(boolean schemaIdHeaderEnabled) {
    this.schemaIdHeaderEnabled = schemaIdHeaderEnabled;
  }

  public boolean isSchemaVersionTruncationEnabled() {
    return schemaVersionTruncationEnabled;
  }

  public void setSchemaVersionTruncationEnabled(boolean schemaVersionTruncationEnabled) {
    this.schemaVersionTruncationEnabled = schemaVersionTruncationEnabled;
  }
}
