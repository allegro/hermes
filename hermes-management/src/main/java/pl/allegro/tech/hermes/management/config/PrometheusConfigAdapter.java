package pl.allegro.tech.hermes.management.config;

import io.micrometer.prometheus.PrometheusConfig;
import java.time.Duration;

public class PrometheusConfigAdapter implements PrometheusConfig {

  private final PrometheusProperties prometheusReporterProperties;

  public PrometheusConfigAdapter(PrometheusProperties prometheusReporterProperties) {
    this.prometheusReporterProperties = prometheusReporterProperties;
  }

  @Override
  public boolean descriptions() {
    return prometheusReporterProperties.getDescriptions();
  }

  @Override
  public Duration step() {
    return prometheusReporterProperties.getStep();
  }

  /** Returning null is fine since we override all the methods from PrometheusConfig. */
  @Override
  public String get(String key) {
    return null; // Nothing to see here, move along.
  }
}
