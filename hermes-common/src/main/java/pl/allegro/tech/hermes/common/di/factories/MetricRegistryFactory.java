package pl.allegro.tech.hermes.common.di.factories;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.google.common.base.Joiner;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterReporter;
import pl.allegro.tech.hermes.common.util.HostnameResolver;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.InetSocketAddress;
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
        MetricRegistry registry = new MetricRegistry();

        if (configFactory.getBooleanProperty(Configs.METRICS_GRAPHITE_REPORTER)) {
            String prefix = Joiner.on(".").join(
                    configFactory.getStringProperty(Configs.GRAPHITE_PREFIX),
                    moduleName,
                    hostnameResolver.resolve().replaceAll("\\.", HermesMetrics.REPLACEMENT_CHAR));

            GraphiteReporter
                    .forRegistry(registry)
                    .prefixedWith(prefix)
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

        return registry;
    }

    @Override
    public void dispose(MetricRegistry instance) {
    }
}
