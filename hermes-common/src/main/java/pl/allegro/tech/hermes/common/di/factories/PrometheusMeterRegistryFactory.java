package pl.allegro.tech.hermes.common.di.factories;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.util.concurrent.TimeUnit;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterReporter;

public class PrometheusMeterRegistryFactory {
    private final MicrometerRegistryParameters parameters;
    private final PrometheusConfig prometheusConfig;
    private final CounterStorage counterStorage;
    private final String prefix;

    public PrometheusMeterRegistryFactory(MicrometerRegistryParameters parameters,
                                          PrometheusConfig prometheusConfig,
                                          CounterStorage counterStorage, String prefix) {
        this.parameters = parameters;
        this.prometheusConfig = prometheusConfig;
        this.counterStorage = counterStorage;
        this.prefix = prefix + "_";
    }

    public PrometheusMeterRegistry provide() {
        PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(prometheusConfig);
        applyFilters(meterRegistry);
        if (parameters.zookeeperReporterEnabled()) {
            registerZookeeperReporter(meterRegistry);
        }
        return meterRegistry;
    }

    private void applyFilters(PrometheusMeterRegistry meterRegistry) {
        meterRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                return id.withName(prefix + id.getName());
            }

            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                return DistributionStatisticConfig.builder().percentiles(parameters.getPercentiles().stream().mapToDouble(Double::doubleValue).toArray()).build().merge(config);
            }
        });
    }

    private void registerZookeeperReporter(PrometheusMeterRegistry meterRegistry) {
        new ZookeeperCounterReporter(meterRegistry, counterStorage, prefix)
                .start(parameters.zookeeperReportPeriod().toSeconds(), TimeUnit.SECONDS);
    }
}
