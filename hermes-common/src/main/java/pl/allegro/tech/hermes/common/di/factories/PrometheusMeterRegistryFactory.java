package pl.allegro.tech.hermes.common.di.factories;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.util.InstanceIdResolver;

public class PrometheusMeterRegistryFactory {
    private final MetricRegistryParameters metricRegistryParameters;
    private final CounterStorage counterStorage;
    private final InstanceIdResolver instanceIdResolver;
    private final PrometheusConfig prometheusConfig;
    private final String moduleName;

    public PrometheusMeterRegistryFactory(MetricRegistryParameters metricRegistryParameters, CounterStorage counterStorage,
                                          InstanceIdResolver instanceIdResolver, PrometheusConfig prometheusConfig,
                                          String moduleName) {
        this.metricRegistryParameters = metricRegistryParameters;
        this.counterStorage = counterStorage;
        this.instanceIdResolver = instanceIdResolver;
        this.prometheusConfig = prometheusConfig;
        this.moduleName = moduleName;
    }

    public PrometheusMeterRegistry provide() {
        return new PrometheusMeterRegistry(prometheusConfig);
    }
}
