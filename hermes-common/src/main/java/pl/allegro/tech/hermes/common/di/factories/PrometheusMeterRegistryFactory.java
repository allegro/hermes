package pl.allegro.tech.hermes.common.di.factories;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class PrometheusMeterRegistryFactory {
    private final MicrometerRegistryParameters micrometerRegistryParameters;
    private final PrometheusConfig prometheusConfig;
    private final String prefix;

    public PrometheusMeterRegistryFactory(MicrometerRegistryParameters micrometerRegistryParameters,
                                          PrometheusConfig prometheusConfig,
                                          String moduleName) {
        this.micrometerRegistryParameters = micrometerRegistryParameters;
        this.prometheusConfig = prometheusConfig;
        this.prefix = moduleName + ".";
    }

    public PrometheusMeterRegistry provide() {
        PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(prometheusConfig);
        meterRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                return id.withName(prefix + id.getName());
            }
        });
        return meterRegistry;
    }
}
