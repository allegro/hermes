package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.ToDoubleFunction;
import pl.allegro.tech.hermes.metrics.HermesTimer;

public class DeadLettersMetrics {
  private final MeterRegistry meterRegistry;
  private final GaugeRegistrar gaugeRegistrar;

  public DeadLettersMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.gaugeRegistrar = new GaugeRegistrar(meterRegistry);
  }

  public <T> void registerConsumerDeadLetterBigqueryQueueSizeGauge(
      T stateObj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(Gauges.DEADLETTER_BIGQUERY_QUEUE_SIZE, stateObj, f);
  }

  public <T> void registerConsumerDeadLetterBigqueryRemainingCapacity(
      T stateObj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(Gauges.DEADLETTER_BIGQUERY_REMAINING_CAPACITY, stateObj, f);
  }

  public HermesTimer deadLettersCommitLatencyTimer() {
    return HermesTimer.from(meterRegistry.timer(Timers.DEADLETTER_BIGQUERY_COMMIT_LATENCY));
  }

  private static class Gauges {
    public static final String DEADLETTER_BIGQUERY_QUEUE_SIZE = "deadletter.bigquery.queue-size";
    public static final String DEADLETTER_BIGQUERY_REMAINING_CAPACITY =
        "deadletter.bigquery.remaining-capacity";
  }

  private static class Timers {
    public static final String DEADLETTER_BIGQUERY_COMMIT_LATENCY =
        "deadletter.bigquery.write-latency";
  }
}
