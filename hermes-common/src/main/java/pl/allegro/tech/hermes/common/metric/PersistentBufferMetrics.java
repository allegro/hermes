package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.Map;

public class PersistentBufferMetrics {
    private final MeterRegistry meterRegistry;
    private final HermesMetrics hermesMetrics;

    public PersistentBufferMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.hermesMetrics = hermesMetrics;
    }

    public void registerBackupStorageSizeGauge(Map<?, ?> map) {
        this.hermesMetrics.registerMessageRepositorySizeGauge(map::size);
        this.meterRegistry.gaugeMapSize("backup-storage.size", Tags.empty(), map);
    }
}
