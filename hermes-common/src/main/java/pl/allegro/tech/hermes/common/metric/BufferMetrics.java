package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.concurrent.ConcurrentMap;

public class BufferMetrics {
    private final MeterRegistry meterRegistry;
    private final HermesMetrics hermesMetrics;

    public BufferMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.hermesMetrics = hermesMetrics;
    }

    public void registerBackupStorageSizeGauge(ConcurrentMap<?, ?> map) {
        this.hermesMetrics.registerMessageRepositorySizeGauge(map::size);
        this.meterRegistry.gaugeMapSize("backup-storage.size", Tags.empty(), map);
    }
}
