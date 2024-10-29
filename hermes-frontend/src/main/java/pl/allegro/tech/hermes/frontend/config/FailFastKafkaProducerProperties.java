package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaProducerParameters;

@ConfigurationProperties(prefix = "frontend.kafka.fail-fast-producer")
public class FailFastKafkaProducerProperties {

  private KafkaProducerParameters local = new FailFastLocalKafkaProducerProperties();

  private KafkaProducerParameters remote = new FailFastRemoteKafkaProducerProperties();

  private Duration speculativeSendDelay = Duration.ofMillis(250);

  private FallbackSchedulerProperties fallbackScheduler = new FallbackSchedulerProperties();

  public Duration getSpeculativeSendDelay() {
    return speculativeSendDelay;
  }

  public void setSpeculativeSendDelay(Duration speculativeSendDelay) {
    this.speculativeSendDelay = speculativeSendDelay;
  }

  public FallbackSchedulerProperties getFallbackScheduler() {
    return fallbackScheduler;
  }

  public void setFallbackScheduler(FallbackSchedulerProperties fallbackScheduler) {
    this.fallbackScheduler = fallbackScheduler;
  }

  public KafkaProducerParameters getLocal() {
    return local;
  }

  public void setLocal(KafkaProducerParameters local) {
    this.local = local;
  }

  public KafkaProducerParameters getRemote() {
    return remote;
  }

  public void setRemote(KafkaProducerParameters remote) {
    this.remote = remote;
  }

  public static class FallbackSchedulerProperties {

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
