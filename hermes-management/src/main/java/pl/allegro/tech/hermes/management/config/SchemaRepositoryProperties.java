package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "schema.repository")
public class SchemaRepositoryProperties {

  private String serverUrl = "http://localhost:8888/";

  private boolean validationEnabled = false;

  private int connectionTimeoutMillis = 1000;

  private int socketTimeoutMillis = 3000;
  private String deleteSchemaPathSuffix = "versions";

  private boolean subjectSuffixEnabled = false;

  private boolean subjectNamespaceEnabled = false;

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public boolean isValidationEnabled() {
    return validationEnabled;
  }

  public void setValidationEnabled(boolean validationEnabled) {
    this.validationEnabled = validationEnabled;
  }

  public int getConnectionTimeoutMillis() {
    return connectionTimeoutMillis;
  }

  public void setConnectionTimeoutMillis(int connectionTimeoutMillis) {
    this.connectionTimeoutMillis = connectionTimeoutMillis;
  }

  public int getSocketTimeoutMillis() {
    return socketTimeoutMillis;
  }

  public String getDeleteSchemaPathSuffix() {
    return deleteSchemaPathSuffix;
  }

  public void setDeleteSchemaPathSuffix(String deleteSchemaPathSuffix) {
    this.deleteSchemaPathSuffix = deleteSchemaPathSuffix;
  }

  public void setSocketTimeoutMillis(int socketTimeoutMillis) {
    this.socketTimeoutMillis = socketTimeoutMillis;
  }

  public boolean isSubjectSuffixEnabled() {
    return subjectSuffixEnabled;
  }

  public void setSubjectSuffixEnabled(boolean subjectSuffixEnabled) {
    this.subjectSuffixEnabled = subjectSuffixEnabled;
  }

  public boolean isSubjectNamespaceEnabled() {
    return subjectNamespaceEnabled;
  }

  public void setSubjectNamespaceEnabled(boolean subjectNamespaceEnabled) {
    this.subjectNamespaceEnabled = subjectNamespaceEnabled;
  }
}
