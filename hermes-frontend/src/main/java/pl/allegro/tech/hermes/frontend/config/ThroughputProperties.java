package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.throughput")
public class ThroughputProperties {

  private long max = Long.MAX_VALUE;

  public long getMax() {
    return max;
  }

  public void setMax(long max) {
    this.max = max;
  }
}
