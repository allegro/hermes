package pl.allegro.tech.hermes.consumers.config;

import java.time.Duration;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.Http2ClientParameters;

public class Http2ClientProperties implements Http2ClientParameters {

  private boolean enabled = true;

  private int threadPoolSize = 10;

  private boolean threadPoolMonitoringEnabled = false;

  private Duration idleTimeout = Duration.ofMillis(0);

  private int maxRequestsQueuedPerDestination = 100;

  private boolean followRedirectsEnabled = false;

  private Duration connectionTimeout = Duration.ofSeconds(15);

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public int getThreadPoolSize() {
    return threadPoolSize;
  }

  public void setThreadPoolSize(int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
  }

  @Override
  public boolean isThreadPoolMonitoringEnabled() {
    return threadPoolMonitoringEnabled;
  }

  public void setThreadPoolMonitoringEnabled(boolean threadPoolMonitoringEnabled) {
    this.threadPoolMonitoringEnabled = threadPoolMonitoringEnabled;
  }

  @Override
  public Duration getIdleTimeout() {
    return idleTimeout;
  }

  public void setIdleTimeout(Duration idleTimeout) {
    this.idleTimeout = idleTimeout;
  }

  @Override
  public int getMaxRequestsQueuedPerDestination() {
    return maxRequestsQueuedPerDestination;
  }

  public void setMaxRequestsQueuedPerDestination(int maxRequestsQueuedPerDestination) {
    this.maxRequestsQueuedPerDestination = maxRequestsQueuedPerDestination;
  }

  @Override
  public boolean isFollowRedirectsEnabled() {
    return followRedirectsEnabled;
  }

  public void setFollowRedirectsEnabled(boolean followRedirectsEnabled) {
    this.followRedirectsEnabled = followRedirectsEnabled;
  }

  @Override
  public Duration getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(Duration connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }
}
