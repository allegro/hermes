package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.function.ToDoubleFunction;

public class GaugeRegistrar {
  private final MeterRegistry meterRegistry;

  public GaugeRegistrar(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public <T> void registerGauge(String name, T stateObj, ToDoubleFunction<T> f) {
    registerGauge(name, stateObj, f, Tags.empty());
  }

  public <T> void registerGauge(
      String name, T stateObj, ToDoubleFunction<T> f, Iterable<Tag> tags) {
    meterRegistry.gauge(name, tags, stateObj, f);
  }
}
