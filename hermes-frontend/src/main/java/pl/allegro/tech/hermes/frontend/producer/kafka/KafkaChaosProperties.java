package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.kafka.chaos")
public class KafkaChaosProperties {
  private ChaosSchedulerProperties chaosScheduler = new ChaosSchedulerProperties();

  public ChaosSchedulerProperties getChaosScheduler() {
    return chaosScheduler;
  }

  public void setChaosScheduler(ChaosSchedulerProperties chaosScheduler) {
    this.chaosScheduler = chaosScheduler;
  }

  public static class ChaosSchedulerProperties {

    private int threadPoolSize = 16;

    private boolean threadPoolMonitoringEnabled = false;

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
}
