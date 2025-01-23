package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.ToDoubleFunction;
import pl.allegro.tech.hermes.metrics.HermesTimer;

public class TrackerElasticSearchMetrics {
  private final MeterRegistry meterRegistry;
  private final GaugeRegistrar gaugeRegistrar;

  public TrackerElasticSearchMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.gaugeRegistrar = new GaugeRegistrar(meterRegistry);
  }

  public <T> void registerProducerTrackerElasticSearchQueueSizeGauge(
      T stateObj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(Gauges.TRACKER_ELASTICSEARCH_QUEUE_SIZE, stateObj, f);
  }

  public <T> void registerProducerTrackerElasticSearchRemainingCapacity(
      T stateObj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(Gauges.TRACKER_ELASTICSEARCH_REMAINING_CAPACITY, stateObj, f);
  }

  public <T> void registerConsumerTrackerElasticSearchQueueSizeGauge(
      T stateObj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(Gauges.TRACKER_ELASTICSEARCH_QUEUE_SIZE, stateObj, f);
  }

  public <T> void registerConsumerTrackerElasticSearchRemainingCapacity(
      T stateObj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(Gauges.TRACKER_ELASTICSEARCH_REMAINING_CAPACITY, stateObj, f);
  }

  public HermesTimer trackerElasticSearchCommitLatencyTimer() {
    return HermesTimer.from(meterRegistry.timer(Timers.ELASTICSEARCH_COMMIT_LATENCY));
  }

  private static class Gauges {
    public static final String TRACKER_ELASTICSEARCH_QUEUE_SIZE =
        "tracker.elasticsearch.queue-size";
    public static final String TRACKER_ELASTICSEARCH_REMAINING_CAPACITY =
        "tracker.elasticsearch.remaining-capacity";
  }

  private static class Timers {
    public static final String ELASTICSEARCH_COMMIT_LATENCY =
        "tracker.elasticsearch.commit-latency";
  }
}
