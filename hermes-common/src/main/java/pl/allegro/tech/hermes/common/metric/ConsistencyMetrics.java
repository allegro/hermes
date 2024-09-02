package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.ToDoubleFunction;

public class ConsistencyMetrics {
  private final MeterRegistry meterRegistry;

  ConsistencyMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public <T> void registerStorageConsistencyGauge(
      T stateObject, ToDoubleFunction<T> valueFunction) {
    meterRegistry.gauge("storage.consistency", stateObject, valueFunction);
  }
}
