package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import pl.allegro.tech.hermes.common.di.factories.ZookeeperParameters;

public class ZookeeperProperties implements ZookeeperParameters {

  private String connectionString = "localhost:2181";

  private String datacenter = "dc";

  private Duration baseSleepTime = Duration.ofMillis(1000);

  private Duration maxSleepTime = Duration.ofSeconds(30);

  private int maxRetries = 29;

  private Duration connectionTimeout = Duration.ofSeconds(10);

  private Duration sessionTimeout = Duration.ofSeconds(10_000);

  private String root = "/hermes";

  private int processingThreadPoolSize = 5;

  private ZookeeperAuthorizationProperties authorization = new ZookeeperAuthorizationProperties();

  @Override
  public String getConnectionString() {
    return connectionString;
  }

  public void setConnectionString(String connectionString) {
    this.connectionString = connectionString;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public void setDatacenter(String datacenter) {
    this.datacenter = datacenter;
  }

  @Override
  public Duration getBaseSleepTime() {
    return baseSleepTime;
  }

  public void setBaseSleepTime(Duration baseSleepTime) {
    this.baseSleepTime = baseSleepTime;
  }

  @Override
  public Duration getMaxSleepTime() {
    return maxSleepTime;
  }

  public void setMaxSleepTime(Duration maxSleepTime) {
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
  public Duration getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(Duration connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  @Override
  public Duration getSessionTimeout() {
    return sessionTimeout;
  }

  public void setSessionTimeout(Duration sessionTimeout) {
    this.sessionTimeout = sessionTimeout;
  }

  @Override
  public String getRoot() {
    return root;
  }

  public void setRoot(String root) {
    this.root = root;
  }

  @Override
  public int getProcessingThreadPoolSize() {
    return processingThreadPoolSize;
  }

  @Override
  public boolean isAuthorizationEnabled() {
    return authorization.enabled;
  }

  @Override
  public String getScheme() {
    return authorization.scheme;
  }

  @Override
  public String getUser() {
    return authorization.user;
  }

  @Override
  public String getPassword() {
    return authorization.password;
  }

  public void setProcessingThreadPoolSize(int processingThreadPoolSize) {
    this.processingThreadPoolSize = processingThreadPoolSize;
  }

  public ZookeeperAuthorizationProperties getAuthorization() {
    return authorization;
  }

  public void setAuthorization(ZookeeperAuthorizationProperties authorization) {
    this.authorization = authorization;
  }

  public static class ZookeeperAuthorizationProperties {

    private boolean enabled = false;

    private String scheme = "digest";

    private String user = "user";

    private String password = "password";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getScheme() {
      return scheme;
    }

    public void setScheme(String scheme) {
      this.scheme = scheme;
    }

    public String getUser() {
      return user;
    }

    public void setUser(String user) {
      this.user = user;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
