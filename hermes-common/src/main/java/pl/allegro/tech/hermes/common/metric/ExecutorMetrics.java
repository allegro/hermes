package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public class ExecutorMetrics {
  private final MeterRegistry meterRegistry;

  public ExecutorMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public ExecutorService monitor(ExecutorService executorService, String executorName) {
    return ExecutorServiceMetrics.monitor(meterRegistry, executorService, executorName);
  }

  public ScheduledExecutorService monitor(
      ScheduledExecutorService scheduledExecutorService, String executorName) {
    return ExecutorServiceMetrics.monitor(meterRegistry, scheduledExecutorService, executorName);
  }
}
