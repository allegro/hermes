package pl.allegro.tech.hermes.management.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "metrics.micrometer")
public class MicrometerRegistryProperties {

  private List<Double> percentiles = List.of(0.5, 0.99, 0.999);

  public List<Double> getPercentiles() {
    return percentiles;
  }

  public void setPercentiles(List<Double> percentiles) {
    this.percentiles = percentiles;
  }
}
