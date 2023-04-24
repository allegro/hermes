package pl.allegro.tech.hermes.frontend.config;

import com.codahale.metrics.MetricRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;

public class FrontendDropwizardMeterRegistry extends DropwizardMeterRegistry  {

    public FrontendDropwizardMeterRegistry(DropwizardConfig config, MetricRegistry registry,
                                           HierarchicalNameMapper nameMapper, Clock clock) {
        super(config, registry, nameMapper, clock);
    }

    /**
     * This behaves like the good old codahale metric registry.
     * Graphite doesn't report gauges which return null.
     */
    @Override
    protected Double nullGaugeValue() {
        return null;
    }
}
