package pl.allegro.tech.hermes.common.di.factories;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class PrometheusMeterRegistryFactory {
    private final MicrometerRegistryParameters parameters;
    private final PrometheusConfig prometheusConfig;
    private final String prefix;

    public PrometheusMeterRegistryFactory(MicrometerRegistryParameters parameters,
                                          PrometheusConfig prometheusConfig,
                                          String prefix) {
        this.parameters = parameters;
        this.prometheusConfig = prometheusConfig;
        this.prefix = prefix + ".";
    }

    public PrometheusMeterRegistry provide() {
        PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(prometheusConfig);
        meterRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                return id.withName(prefix + id.getName());
            }

            @Override
            public DistributionStatisticConfig configure(Meter.Id id,
                                                         DistributionStatisticConfig config) {
                return DistributionStatisticConfig.builder()
                        .percentiles(parameters.getPercentiles().stream().mapToDouble(Double::doubleValue).toArray())
                        .build()
                        .merge(config);
            }
        });
        return meterRegistry;
    }
}
