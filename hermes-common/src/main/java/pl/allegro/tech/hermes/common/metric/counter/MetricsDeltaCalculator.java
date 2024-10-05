package pl.allegro.tech.hermes.common.metric.counter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Small util to calculate delta between previous and current state of metric. It helps in
 * developing incremental metric storage, as Metrics always wants to push current state of Metric,
 * not increment.
 */
public class MetricsDeltaCalculator {

  private final ConcurrentMap<String, Long> previousValues = new ConcurrentHashMap<>();

  public long calculateDelta(String metricName, Long currentValue) {
    Long previousValue = previousValues.put(metricName, currentValue);

    long delta = currentValue;
    if (previousValue != null) {
      delta = currentValue - previousValue;
    }
    return delta;
  }

  public void revertDelta(String metricName, Long delta) {
    Long previousValue = previousValues.get(metricName);
    if (previousValue != null) {
      previousValues.put(metricName, previousValue - delta);
    }
  }

  public void clear() {
    previousValues.clear();
  }
}
