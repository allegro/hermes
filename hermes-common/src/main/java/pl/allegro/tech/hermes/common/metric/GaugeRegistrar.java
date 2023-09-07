package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import java.util.function.ToDoubleFunction;

public class GaugeRegistrar {
    private final MeterRegistry meterRegistry;
    private final HermesMetrics hermesMetrics;

    public GaugeRegistrar(MeterRegistry meterRegistry, HermesMetrics hermesMetrics) {
        this.meterRegistry = meterRegistry;
        this.hermesMetrics = hermesMetrics;
    }

    public <T> void registerGauge(String graphiteName,
                                  String prometheusName,
                                  T stateObj,
                                  ToDoubleFunction<T> f,
                                  Iterable<Tag> tags) {
        meterRegistry.gauge(prometheusName, tags, stateObj, f);
        hermesMetrics.registerGauge(graphiteName, () -> f.applyAsDouble(stateObj));
    }

    public <T> void registerGauge(String graphiteName,
                                  String prometheusName,
                                  T stateObj,
                                  ToDoubleFunction<T> f) {
        registerGauge(graphiteName, prometheusName, stateObj, f, Tags.empty());
    }

    public <T> void registerGauge(String name,
                                  T stateObj,
                                  ToDoubleFunction<T> f) {
        registerGauge(name, name, stateObj, f);
    }
}

