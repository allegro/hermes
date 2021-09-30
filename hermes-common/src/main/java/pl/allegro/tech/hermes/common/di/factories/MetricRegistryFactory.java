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
import pl.allegro.tech.hermes.common.util.InstanceIdResolver;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MetricRegistryFactory implements Factory<MetricRegistry> {

    private final ConfigFactory configFactory;
    private final CounterStorage counterStorage;
    private final InstanceIdResolver instanceIdResolver;
    private final String moduleName;

    @Inject
    public MetricRegistryFactory(ConfigFactory configFactory,
                                 CounterStorage counterStorage,
                                 InstanceIdResolver instanceIdResolver,
                                 @Named("moduleName") String moduleName) {
        this.configFactory = configFactory;
        this.counterStorage = counterStorage;
        this.instanceIdResolver = instanceIdResolver;
        this.moduleName = moduleName;

    }

    @Override
    public MetricRegistry provide() {
        MetricRegistry registry = createMetricsRegistry();

        if (configFactory.getBooleanProperty(Configs.METRICS_GRAPHITE_REPORTER)) {
            String prefix = Joiner.on(".").join(
                    configFactory.getStringProperty(Configs.GRAPHITE_PREFIX),
                    moduleName,
                    instanceIdResolver.resolve().replaceAll("\\.", HermesMetrics.REPLACEMENT_CHAR));

            GraphiteReporter
                    .forRegistry(registry)
                    .prefixedWith(prefix)
                    .disabledMetricAttributes(getDisabledAttributesFromConfig())
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

    private Set<MetricAttribute> getDisabledAttributesFromConfig() {
        Map<Configs, MetricAttribute> configsMetricAttributeMap = new HashMap<>();
        Set<MetricAttribute> disabledAttributes = Sets.newHashSet();

        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_MAX, MetricAttribute.MAX);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_MEAN, MetricAttribute.MEAN);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_MIN, MetricAttribute.MIN);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_STDDEV, MetricAttribute.STDDEV);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_P50, MetricAttribute.P50);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_P75, MetricAttribute.P75);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_P95, MetricAttribute.P95);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_P98, MetricAttribute.P98);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_P99, MetricAttribute.P99);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_P999, MetricAttribute.P999);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_COUNT, MetricAttribute.COUNT);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_M1_RATE, MetricAttribute.M1_RATE);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_M5_RATE, MetricAttribute.M5_RATE);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_M15_RATE, MetricAttribute.M15_RATE);
        configsMetricAttributeMap.put(Configs.METRICS_ATTRIBUTE_MEAN_RATE, MetricAttribute.MEAN_RATE);

        configsMetricAttributeMap.forEach((k, v) -> {
            if(!configFactory.getBooleanProperty(k)) {
                disabledAttributes.add(v);
            }
        });

        return disabledAttributes;
    }

    @Override
    public void dispose(MetricRegistry instance) {
    }
}
