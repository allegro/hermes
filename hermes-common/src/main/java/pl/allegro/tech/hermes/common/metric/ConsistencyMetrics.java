package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;


public class ConsistencyMetrics {
    private final MeterRegistry meterRegistry;

    ConsistencyMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public <T> void registerStorageConsistencyGauge(boolean isStorageConsistent) {
        double value = isStorageConsistent ? 1 : 0;
        meterRegistry.gauge("storage.consistency", value);
    }
}
