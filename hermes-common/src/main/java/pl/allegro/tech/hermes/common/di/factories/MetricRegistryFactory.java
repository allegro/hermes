package pl.allegro.tech.hermes.common.di.factories;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricAttribute;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.MetricRegistryWithExponentiallyDecayingReservoir;
import pl.allegro.tech.hermes.common.metric.MetricRegistryWithHdrHistogramReservoir;
import pl.allegro.tech.hermes.common.metric.MetricsReservoirType;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterReporter;
import pl.allegro.tech.hermes.common.util.HostnameResolver;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MetricRegistryFactory implements Factory<MetricRegistry> {

    private final ConfigFactory configFactory;
    private final CounterStorage counterStorage;
    private final HostnameResolver hostnameResolver;
    private final String moduleName;

    @Inject
    public MetricRegistryFactory(ConfigFactory configFactory,
                                 CounterStorage counterStorage,
                                 HostnameResolver hostnameResolver,
                                 @Named("moduleName") String moduleName) {
        this.configFactory = configFactory;
        this.counterStorage = counterStorage;
        this.hostnameResolver = hostnameResolver;
        this.moduleName = moduleName;

    }

    @Override
    public MetricRegistry provide() {
        MetricRegistry registry = createMetricsRegistry();

        if (configFactory.getBooleanProperty(Configs.METRICS_GRAPHITE_REPORTER)) {

            Set<MetricAttribute> disabledAttributes = Sets.newHashSet(
                    MetricAttribute.M15_RATE,
                    MetricAttribute.M5_RATE,
                    MetricAttribute.MEAN,
                    MetricAttribute.MEAN_RATE,
                    MetricAttribute.MIN,
                    MetricAttribute.STDDEV
            );

            String prefix = Joiner.on(".").join(
                    configFactory.getStringProperty(Configs.GRAPHITE_PREFIX),
                    moduleName,
                    HermesMetrics.escapeDots(hostnameResolver.resolve()));

            GraphiteReporter
                    .forRegistry(registry)
                    .prefixedWith(prefix)
                    .disabledMetricAttributes(disabledAttributes)
                    .build(new Graphite(new InetSocketAddress(
                            configFactory.getStringProperty(Configs.GRAPHITE_HOST),
                            configFactory.getIntProperty(Configs.GRAPHITE_PORT)
                    )))
                    .start(configFactory.getIntProperty(Configs.REPORT_PERIOD), TimeUnit.SECONDS);
        }
        if (configFactory.getBooleanProperty(Configs.METRICS_CONSOLE_REPORTER)) {
            ConsoleReporter.forRegistry(registry).build().start(
                    configFactory.getIntProperty(Configs.REPORT_PERIOD), TimeUnit.SECONDS
            );
        }

        if (configFactory.getBooleanProperty(Configs.METRICS_ZOOKEEPER_REPORTER)) {
            new ZookeeperCounterReporter(registry, counterStorage, configFactory).start(
                    configFactory.getIntProperty(Configs.REPORT_PERIOD),
                    TimeUnit.SECONDS
            );
        }

        registerJvmMetrics(registry);

        return registry;
    }

    private MetricRegistry createMetricsRegistry() {
        String metricsReservoirType = configFactory.getStringProperty(Configs.METRICS_RESERVOIR_TYPE).toUpperCase();
        switch (MetricsReservoirType.valueOf(metricsReservoirType)) {
            case HDR:
                return new MetricRegistryWithHdrHistogramReservoir();
            case EXPONENTIALLY_DECAYING:
            default:
                return new MetricRegistryWithExponentiallyDecayingReservoir();
        }
    }

    private void registerJvmMetrics(MetricRegistry metricRegistry) {
        registerAll("jvm.gc", new GarbageCollectorMetricSet(), metricRegistry);
        registerAll("jvm.memory", new MemoryUsageGaugeSet(), metricRegistry);
        metricRegistry.register("jvm.descriptors", new FileDescriptorRatioGauge());
    }

    private void registerAll(String prefix, MetricSet metricSet, MetricRegistry registry) {
        for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            if (entry.getValue() instanceof MetricSet) {
                registerAll(prefix + "." + entry.getKey(), (MetricSet) entry.getValue(), registry);
            } else {
                registry.register(prefix + "." + entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void dispose(MetricRegistry instance) {
    }
}
