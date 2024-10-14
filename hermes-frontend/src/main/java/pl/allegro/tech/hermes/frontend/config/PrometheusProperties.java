package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.prometheus")
public class PrometheusProperties {

  private Duration step = Duration.ofMinutes(1);
  private boolean descriptions = true;

  public Duration getStep() {
    return this.step;
  }

  public void setStep(Duration step) {
    this.step = step;
  }

  public boolean isDescriptions() {
    return this.descriptions;
  }

  public void setDescriptions(boolean descriptions) {
    this.descriptions = descriptions;
  }
}
