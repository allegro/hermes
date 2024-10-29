package pl.allegro.tech.hermes.management.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MicrometerRegistryProperties.class)
public class PrometheusConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public PrometheusMeterRegistry micrometerRegistry(
      MicrometerRegistryProperties properties, PrometheusConfig prometheusConfig) {
    return new PrometheusMeterRegistryFactory(properties, prometheusConfig, "hermes-management")
        .provide();
  }

  @Bean
  @ConditionalOnMissingBean
  public PrometheusConfig prometheusConfig(PrometheusProperties properties) {
    return new PrometheusConfigAdapter(properties);
  }

  public static class PrometheusMeterRegistryFactory {
    private final MicrometerRegistryProperties parameters;
    private final PrometheusConfig prometheusConfig;
    private final String prefix;

    public PrometheusMeterRegistryFactory(
        MicrometerRegistryProperties properties, PrometheusConfig prometheusConfig, String prefix) {
      this.parameters = properties;
      this.prometheusConfig = prometheusConfig;
      this.prefix = prefix + "_";
    }

    public PrometheusMeterRegistry provide() {
      PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(prometheusConfig);
      applyFilters(meterRegistry);
      return meterRegistry;
    }

    private void applyFilters(PrometheusMeterRegistry meterRegistry) {
      meterRegistry
          .config()
          .meterFilter(
              new MeterFilter() {
                @Override
                public Meter.Id map(Meter.Id id) {
                  return id.withName(prefix + id.getName());
                }

                @Override
                public DistributionStatisticConfig configure(
                    Meter.Id id, DistributionStatisticConfig config) {
                  return DistributionStatisticConfig.builder()
                      .percentiles(
                          parameters.getPercentiles().stream()
                              .mapToDouble(Double::doubleValue)
                              .toArray())
                      .build()
                      .merge(config);
                }
              });
    }
  }
}
