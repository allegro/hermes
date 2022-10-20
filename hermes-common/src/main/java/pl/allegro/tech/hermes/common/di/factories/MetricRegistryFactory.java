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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.MetricRegistryWithHdrHistogramReservoir;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterReporter;
import pl.allegro.tech.hermes.common.util.InstanceIdResolver;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Named;

public class MetricRegistryFactory {

    private static final Logger logger = LoggerFactory.getLogger(MetricRegistryFactory.class);
    private final MetricRegistryParameters metricRegistryParameters;
    private final GraphiteParameters graphiteParameters;
    private final CounterStorage counterStorage;
    private final InstanceIdResolver instanceIdResolver;
    private final String moduleName;

    public MetricRegistryFactory(MetricRegistryParameters metricRegistryParameters,
                                 GraphiteParameters graphiteParameters,
                                 CounterStorage counterStorage,
                                 InstanceIdResolver instanceIdResolver,
                                 @Named("moduleName") String moduleName) {
        this.metricRegistryParameters = metricRegistryParameters;
        this.graphiteParameters = graphiteParameters;
        this.counterStorage = counterStorage;
        this.instanceIdResolver = instanceIdResolver;
        this.moduleName = moduleName;
    }

    public MetricRegistry provide() {
        MetricRegistry registry = new MetricRegistryWithHdrHistogramReservoir();

        if (metricRegistryParameters.isGraphiteReporterEnabled()) {
            String prefix = Joiner.on(".").join(
                    graphiteParameters.getPrefix(),
                    moduleName,
                    instanceIdResolver.resolve().replaceAll("\\.", HermesMetrics.REPLACEMENT_CHAR));

            GraphiteReporter
                    .forRegistry(registry)
                    .prefixedWith(prefix)
                    .disabledMetricAttributes(getDisabledAttributesFromConfig())
                    .build(new Graphite(new InetSocketAddress(
                            graphiteParameters.getHost(),
                            graphiteParameters.getPort()
                    )))
                    .start(metricRegistryParameters.getReportPeriod().toSeconds(), TimeUnit.SECONDS);
        }
        if (metricRegistryParameters.isConsoleReporterEnabled()) {
            ConsoleReporter.forRegistry(registry).build().start(
                    metricRegistryParameters.getReportPeriod().toSeconds(), TimeUnit.SECONDS
            );
        }

        if (metricRegistryParameters.isZookeeperReporterEnabled()) {
            new ZookeeperCounterReporter(registry, counterStorage, graphiteParameters.getPrefix()).start(
                    metricRegistryParameters.getReportPeriod().toSeconds(),
                    TimeUnit.SECONDS
            );
        }

        registerJvmMetrics(registry);

        return registry;
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
        Set<MetricAttribute> disabledAttributes = Sets.newHashSet();
        String disabledAttributesFromConfig = metricRegistryParameters.getDisabledAttributes();
        List<String> disabledAttributesList = Arrays.asList(disabledAttributesFromConfig.split("\\s*,\\s*"));

        disabledAttributesList.forEach(singleAttribute -> {
            try {
                disabledAttributes.add(MetricAttribute.valueOf(singleAttribute));
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to add disabled attribute from config: {}", e.getMessage());
            }
        });

        return disabledAttributes;
    }
}
