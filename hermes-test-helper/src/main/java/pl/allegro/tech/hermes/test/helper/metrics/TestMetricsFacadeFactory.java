package pl.allegro.tech.hermes.test.helper.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;

public class TestMetricsFacadeFactory {

  public static MetricsFacade create() {
    return new MetricsFacade(new SimpleMeterRegistry());
  }
}
