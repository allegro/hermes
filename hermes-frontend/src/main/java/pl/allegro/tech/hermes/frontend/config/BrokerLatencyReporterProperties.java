package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.broker-latency-reporter")
public class BrokerLatencyReporterProperties {
  private boolean enabled;
  private Duration slowResponseLoggingThreshold = Duration.ofMillis(100);
  private int threadPoolSize = 8;
  private int threadPoolQueueCapacity = 1_000_000;

  public int getThreadPoolSize() {
    return threadPoolSize;
  }

  public void setThreadPoolSize(int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
  }

  public int getThreadPoolQueueCapacity() {
    return threadPoolQueueCapacity;
  }

  public void setThreadPoolQueueCapacity(int threadPoolQueueCapacity) {
    this.threadPoolQueueCapacity = threadPoolQueueCapacity;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Duration getSlowResponseLoggingThreshold() {
    return slowResponseLoggingThreshold;
  }

  public void setSlowResponseLoggingThreshold(Duration slowResponseLoggingThreshold) {
    this.slowResponseLoggingThreshold = slowResponseLoggingThreshold;
  }
}
