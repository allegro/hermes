package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.function.ToDoubleFunction;

import static pl.allegro.tech.hermes.common.metric.Gauges.BACKUP_STORAGE_SIZE;

public class PersistentBufferMetrics {
    private final MeterRegistry meterRegistry;
    private final HermesMetrics hermesMetrics;

    public PersistentBufferMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.hermesMetrics = hermesMetrics;
    }

    public <T> void registerBackupStorageSizeGauge(T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerMessageRepositorySizeGauge(() -> (int) f.applyAsDouble(obj));
        meterRegistry.gauge(BACKUP_STORAGE_SIZE, obj, f);
    }
}
