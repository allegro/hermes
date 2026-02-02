package pl.allegro.tech.hermes.management.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.management.domain.consistency.ConsistencyCheckerParameters;

@ConfigurationProperties(prefix = "consistency-checker")
public class ConsistencyCheckerProperties implements ConsistencyCheckerParameters {

  private int threadPoolSize = 2;
  private boolean periodicCheckEnabled = false;
  private Duration refreshInterval = Duration.ofMinutes(15);
  private Duration initialRefreshDelay = Duration.ofMinutes(2);

  @Override
  public int getThreadPoolSize() {
    return threadPoolSize;
  }

  public void setThreadPoolSize(int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
  }

  @Override
  public boolean isPeriodicCheckEnabled() {
    return periodicCheckEnabled;
  }

  public void setPeriodicCheckEnabled(boolean periodicCheckEnabled) {
    this.periodicCheckEnabled = periodicCheckEnabled;
  }

  @Override
  public Duration getRefreshInterval() {
    return refreshInterval;
  }

  public void setRefreshInterval(Duration refreshInterval) {
    this.refreshInterval = refreshInterval;
  }

  @Override
  public Duration getInitialRefreshDelay() {
    return initialRefreshDelay;
  }

  public void setInitialRefreshDelay(Duration initialRefreshDelay) {
    this.initialRefreshDelay = initialRefreshDelay;
  }
}
