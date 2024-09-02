package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.readiness.check")
public class ReadinessCheckProperties {

  private boolean enabled = false;

  private boolean kafkaCheckEnabled = false;

  private Duration interval = Duration.ofSeconds(1);

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Duration getInterval() {
    return interval;
  }

  public void setIntervalSeconds(Duration intervalSeconds) {
    this.interval = intervalSeconds;
  }

  public boolean isKafkaCheckEnabled() {
    return kafkaCheckEnabled;
  }

  public void setKafkaCheckEnabled(boolean kafkaCheckEnabled) {
    this.kafkaCheckEnabled = kafkaCheckEnabled;
  }
}
