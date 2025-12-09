package pl.allegro.tech.hermes.management.config.storage;

import java.time.Duration;
import pl.allegro.tech.hermes.common.di.factories.ZookeeperParameters;

public class StorageProperties implements ZookeeperParameters {
  private String datacenter = "dc";
  private String connectionString = "localhost:2181";
  private String root = "/hermes";
  private int sessionTimeout = 10000;
  private int connectionTimeout = 1000;
  private int baseSleepTime = 1000000;
  private int maxSleepTime = 30000;
  private int maxRetries = 29;
  private int processingThreadPoolSize = 5;

  private StorageAuthorizationProperties authorization;

  public String getConnectionString() {
    return connectionString;
  }

  public void setConnectionString(String connectionString) {
    this.connectionString = connectionString;
  }

  @Override
  public Duration getSessionTimeout() {
    return Duration.ofMillis(sessionTimeout);
  }

  public void setSessionTimeout(int sessionTimeout) {
    this.sessionTimeout = sessionTimeout;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public void setDatacenter(String datacenter) {
    this.datacenter = datacenter;
  }

  public void setAuthorization(StorageAuthorizationProperties authorization) {
    this.authorization = authorization;
  }

  public StorageAuthorizationProperties getAuthorization() {
    return authorization;
  }

  @Override
  public boolean isAuthorizationEnabled() {
    return authorization != null;
  }

  @Override
  public Duration getBaseSleepTime() {
    return Duration.ofMillis(baseSleepTime);
  }

  public void setBaseSleepTime(int baseSleepTime) {
    this.baseSleepTime = baseSleepTime;
  }

  @Override
  public Duration getConnectionTimeout() {
    return Duration.ofMillis(connectionTimeout);
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  @Override
  public Duration getMaxSleepTime() {
    return Duration.ofMillis(maxSleepTime);
  }

  public void setMaxSleepTime(int maxSleepTime) {
    this.maxSleepTime = maxSleepTime;
  }

  @Override
  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  @Override
  public int getProcessingThreadPoolSize() {
    return processingThreadPoolSize;
  }

  public void setProcessingThreadPoolSize(int processingThreadPoolSize) {
    this.processingThreadPoolSize = processingThreadPoolSize;
  }

  @Override
  public String getPassword() {
    return authorization.getPassword();
  }

  @Override
  public String getRoot() {
    return root;
  }

  public void setRoot(String root) {
    this.root = root;
  }

  @Override
  public String getScheme() {
    return authorization.getScheme();
  }

  @Override
  public String getUser() {
    return authorization.getUser();
  }
}
