package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "management.health")
public class HealthProperties {

  private long periodSeconds = 30L;
  private boolean enabled = false;

  public long getPeriodSeconds() {
    return periodSeconds;
  }

  public void setPeriodSeconds(long periodSeconds) {
    this.periodSeconds = periodSeconds;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
