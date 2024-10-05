package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.common.metric.Gauges.BACKUP_STORAGE_SIZE;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.ToDoubleFunction;

public class PersistentBufferMetrics {
  private final MeterRegistry meterRegistry;

  public PersistentBufferMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public <T> void registerBackupStorageSizeGauge(T obj, ToDoubleFunction<T> f) {
    meterRegistry.gauge(BACKUP_STORAGE_SIZE, obj, f);
  }
}
