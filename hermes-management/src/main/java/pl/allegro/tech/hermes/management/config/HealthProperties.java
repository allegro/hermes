package pl.allegro.tech.hermes.management.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "management.health")
public class HealthProperties {

  private Duration period = Duration.ofSeconds(30);
  private boolean enabled = false;

  public Duration getPeriod() {
    return period;
  }

  public void setPeriod(Duration period) {
    this.period = period;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
