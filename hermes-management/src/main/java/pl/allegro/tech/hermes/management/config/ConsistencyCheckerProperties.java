package pl.allegro.tech.hermes.management.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consistency-checker")
public class ConsistencyCheckerProperties {

  private int threadPoolSize = 2;
  private boolean periodicCheckEnabled = false;
  private Duration refreshInterval = Duration.ofMinutes(15);
  private Duration initialRefreshDelay = Duration.ofMinutes(2);

  public int getThreadPoolSize() {
    return threadPoolSize;
  }

  public void setThreadPoolSize(int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
  }

  public boolean isPeriodicCheckEnabled() {
    return periodicCheckEnabled;
  }

  public void setPeriodicCheckEnabled(boolean periodicCheckEnabled) {
    this.periodicCheckEnabled = periodicCheckEnabled;
  }

  public Duration getRefreshInterval() {
    return refreshInterval;
  }

  public void setRefreshInterval(Duration refreshInterval) {
    this.refreshInterval = refreshInterval;
  }

  public Duration getInitialRefreshDelay() {
    return initialRefreshDelay;
  }

  public void setInitialRefreshDelay(Duration initialRefreshDelay) {
    this.initialRefreshDelay = initialRefreshDelay;
  }
}
