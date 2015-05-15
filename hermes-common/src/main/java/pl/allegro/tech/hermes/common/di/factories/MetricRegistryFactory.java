package pl.allegro.tech.hermes.common.di.factories;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;

import javax.inject.Inject;
import java.util.Optional;

import static pl.allegro.tech.hermes.common.config.Configs.METRICS_REGISTRY_NAME;

public class MetricRegistryFactory implements Factory<MetricRegistry> {

    private final ConfigFactory configFactory;

    @Inject
    public MetricRegistryFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    @Override
    public MetricRegistry provide() {
        Optional<String> metricRegistryName = Optional.ofNullable(configFactory.getStringProperty(METRICS_REGISTRY_NAME));

        return metricRegistryName.isPresent() ?
            SharedMetricRegistries.getOrCreate(configFactory.getStringProperty(METRICS_REGISTRY_NAME)) : new MetricRegistry();
    }

    @Override
    public void dispose(MetricRegistry instance) {
        SharedMetricRegistries.remove(configFactory.getStringProperty(METRICS_REGISTRY_NAME));
    }
}
