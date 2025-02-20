package pl.allegro.tech.hermes.client.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MicrometerTaggedMetricsProvider implements MetricsProvider {

  private final MeterRegistry metrics;

  public MicrometerTaggedMetricsProvider(MeterRegistry metrics) {
    this.metrics = metrics;
  }

  @Override
  public void counterIncrement(String topic, String key) {
    counterIncrement(topic, key, new HashMap<>());
  }

  @Override
  public void counterIncrement(String topic, String key, Map<String, String> tags) {
    tags.put("topic", topic);
    metrics
        .counter(
            buildCounterName(key),
            Tags.of(
                tags.entrySet().stream()
                    .map(e -> Tag.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toSet())))
        .increment();
  }

  @Override
  public void timerRecord(String topic, String key, long duration, TimeUnit unit) {
    Map<String, String> tags = new HashMap<>();
    tags.put("topic", topic);
    metrics
        .timer(
            buildCounterName(key),
            Tags.of(
                tags.entrySet().stream()
                    .map(e -> Tag.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toSet())))
        .record(duration, unit);
  }

  @Override
  public void histogramUpdate(String topic, String key, int value) {
    Map<String, String> tags = new HashMap<>();
    tags.put("topic", topic);
    metrics
        .summary(
            buildCounterName(key),
            Tags.of(
                tags.entrySet().stream()
                    .map(e -> Tag.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toSet())))
        .record(value);
  }

  private String buildCounterName(String key) {
    return prefix + key;
  }
}
