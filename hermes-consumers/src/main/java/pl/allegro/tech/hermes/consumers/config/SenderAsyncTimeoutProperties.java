package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.sender.async.timeout")
public class SenderAsyncTimeoutProperties {

  private int milliseconds = 5_000;

  private int threadPoolSize = 32;

  private boolean threadPoolMonitoringEnabled = false;

  public int getMilliseconds() {
    return milliseconds;
  }

  public void setMilliseconds(int milliseconds) {
    this.milliseconds = milliseconds;
  }

  public int getThreadPoolSize() {
    return threadPoolSize;
  }

  public void setThreadPoolSize(int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
  }

  public boolean isThreadPoolMonitoringEnabled() {
    return threadPoolMonitoringEnabled;
  }

  public void setThreadPoolMonitoringEnabled(boolean threadPoolMonitoringEnabled) {
    this.threadPoolMonitoringEnabled = threadPoolMonitoringEnabled;
  }
}
