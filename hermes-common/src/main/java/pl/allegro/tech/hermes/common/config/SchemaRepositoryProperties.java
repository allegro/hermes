package pl.allegro.tech.hermes.common.config;

import java.time.Duration;

public class SchemaRepositoryProperties {

  private String serverUrl = "http://localhost:8888/";

  private Duration httpReadTimeout = Duration.ofSeconds(2);

  private Duration httpConnectTimeout = Duration.ofSeconds(2000);

  private double onlineCheckPermitsPerSecond = 100.0;

  private Duration onlineCheckAcquireWait = Duration.ofMillis(500);

  private boolean subjectSuffixEnabled = false;

  private boolean subjectNamespaceEnabled = false;

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public Duration getHttpReadTimeout() {
    return httpReadTimeout;
  }

  public void setHttpReadTimeout(Duration httpReadTimeout) {
    this.httpReadTimeout = httpReadTimeout;
  }

  public Duration getHttpConnectTimeout() {
    return httpConnectTimeout;
  }

  public void setHttpConnectTimeout(Duration httpConnectTimeout) {
    this.httpConnectTimeout = httpConnectTimeout;
  }

  public double getOnlineCheckPermitsPerSecond() {
    return onlineCheckPermitsPerSecond;
  }

  public void setOnlineCheckPermitsPerSecond(double onlineCheckPermitsPerSecond) {
    this.onlineCheckPermitsPerSecond = onlineCheckPermitsPerSecond;
  }

  public Duration getOnlineCheckAcquireWait() {
    return onlineCheckAcquireWait;
  }

  public void setOnlineCheckAcquireWait(Duration onlineCheckAcquireWait) {
    this.onlineCheckAcquireWait = onlineCheckAcquireWait;
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
