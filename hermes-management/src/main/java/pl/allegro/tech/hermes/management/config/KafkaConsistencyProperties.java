package pl.allegro.tech.hermes.management.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "management.consistency.kafka")
public class KafkaConsistencyProperties {

  private boolean enabled = false;
  private boolean periodicCheckEnabled = false;
  private Duration initialRefreshDelay = Duration.ofMinutes(2);
  private Duration refreshInterval = Duration.ofMinutes(15);
  private int threadPoolSize = 4;
  private int syncBatchSize = 100;
  private boolean bootstrapStrictPartitions = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isPeriodicCheckEnabled() {
    return periodicCheckEnabled;
  }

  public void setPeriodicCheckEnabled(boolean periodicCheckEnabled) {
    this.periodicCheckEnabled = periodicCheckEnabled;
  }

  public Duration getInitialRefreshDelay() {
    return initialRefreshDelay;
  }

  public void setInitialRefreshDelay(Duration initialRefreshDelay) {
    this.initialRefreshDelay = initialRefreshDelay;
  }

  public Duration getRefreshInterval() {
    return refreshInterval;
  }

  public void setRefreshInterval(Duration refreshInterval) {
    this.refreshInterval = refreshInterval;
  }

  public int getThreadPoolSize() {
    return threadPoolSize;
  }

  public void setThreadPoolSize(int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
  }

  public int getSyncBatchSize() {
    return syncBatchSize;
  }

  public void setSyncBatchSize(int syncBatchSize) {
    this.syncBatchSize = syncBatchSize;
  }

  public boolean isBootstrapStrictPartitions() {
    return bootstrapStrictPartitions;
  }

  public void setBootstrapStrictPartitions(boolean bootstrapStrictPartitions) {
    this.bootstrapStrictPartitions = bootstrapStrictPartitions;
  }
}
