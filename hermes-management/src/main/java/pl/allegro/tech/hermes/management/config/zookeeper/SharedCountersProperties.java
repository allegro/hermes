package pl.allegro.tech.hermes.management.config.zookeeper;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.shared-counters")
public class SharedCountersProperties {

  private int retryTimes = 3;
  private Duration retrySleep = Duration.ofSeconds(1);
  private Duration expiration = Duration.ofHours(72);

  public int getRetryTimes() {
    return retryTimes;
  }

  public void setRetryTimes(int retryTimes) {
    this.retryTimes = retryTimes;
  }

  public Duration getRetrySleep() {
    return retrySleep;
  }

  public void setRetrySleep(Duration retrySleep) {
    this.retrySleep = retrySleep;
  }

  public Duration getExpiration() {
    return expiration;
  }

  public void setExpiration(Duration expiration) {
    this.expiration = expiration;
  }
}
