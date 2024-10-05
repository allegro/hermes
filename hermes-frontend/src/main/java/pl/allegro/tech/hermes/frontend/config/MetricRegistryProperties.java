package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.metrics.metric-registry")
public class MetricRegistryProperties {

  private Duration counterExpireAfterAccess = Duration.ofHours(72);

  public Duration getCounterExpireAfterAccess() {
    return counterExpireAfterAccess;
  }

  public void setCounterExpireAfterAccess(Duration counterExpireAfterAccess) {
    this.counterExpireAfterAccess = counterExpireAfterAccess;
  }
}
