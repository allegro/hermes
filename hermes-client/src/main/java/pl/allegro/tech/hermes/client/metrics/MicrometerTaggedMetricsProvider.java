package pl.allegro.tech.hermes.client.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MicrometerTaggedMetricsProvider implements MetricsProvider {

  static final Map<String, String> METRIC_DESCRIPTIONS =
      Map.ofEntries(
          Map.entry(
              "latency",
              "Time spent sending a message to Hermes, recorded for every publish attempt"
                  + " including retries."),
          Map.entry(
              "status",
              "Total number of HTTP responses received from Hermes, partitioned by HTTP status"
                  + " code."),
          Map.entry(
              "publish.failure",
              "Total number of individual publish attempts that received a failure (non-2xx) HTTP"
                  + " response."),
          Map.entry(
              "failure",
              "Total number of failed publish attempts (exceptions or non-2xx responses), including"
                  + " each failed retry."),
          Map.entry(
              "retries.count",
              "Total number of retry attempts triggered by a failed previous attempt (exception or"
                  + " response matching retry condition)."),
          Map.entry(
              "publish.retry.failure",
              "Total number of individual retry attempts that resulted in a failure response."),
          Map.entry(
              "retries.success",
              "Total number of messages for which the publish completed without exhausting all"
                  + " retries (including first-attempt successes)."),
          Map.entry(
              "retries.attempts",
              "Distribution of the number of retry attempts per message. Recorded when the publish"
                  + " completes without exhausting all retries — value is 0 when no retries were"
                  + " needed. Not recorded when all retries are exhausted."),
          Map.entry(
              "publish.attempt",
              "Total number of messages for which the publish process has completed (either"
                  + " successfully or after exhausting all retries)."),
          Map.entry(
              "publish.retry.success",
              "Total number of messages that were published successfully after at least one"
                  + " retry."),
          Map.entry(
              "publish.finally.success",
              "Total number of messages that were ultimately published successfully (with or"
                  + " without retries)."),
          Map.entry(
              "publish.finally.failure",
              "Total number of messages that ultimately failed to be published (after all retries"
                  + " were exhausted or a non-2xx response was final)."),
          Map.entry(
              "publish.retry.attempt",
              "Total number of messages for which at least one retry was attempted, regardless of"
                  + " the final outcome."),
          Map.entry(
              "retries.exhausted",
              "Total number of messages for which all retry attempts were exhausted without"
                  + " success."));

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
    Counter.builder(buildMetricName(key))
        .description(METRIC_DESCRIPTIONS.get(key))
        .tags(toTags(tags))
        .register(metrics)
        .increment();
  }

  @Override
  public void timerRecord(String topic, String key, long duration, TimeUnit unit) {
    Map<String, String> tags = new HashMap<>();
    tags.put("topic", topic);
    Timer.builder(buildMetricName(key))
        .description(METRIC_DESCRIPTIONS.get(key))
        .tags(toTags(tags))
        .register(metrics)
        .record(duration, unit);
  }

  @Override
  public void histogramUpdate(String topic, String key, int value) {
    Map<String, String> tags = new HashMap<>();
    tags.put("topic", topic);
    DistributionSummary.builder(buildMetricName(key))
        .description(METRIC_DESCRIPTIONS.get(key))
        .tags(toTags(tags))
        .register(metrics)
        .record(value);
  }

  private String buildMetricName(String key) {
    return prefix + key;
  }

  private Tags toTags(Map<String, String> tags) {
    return Tags.of(
        tags.entrySet().stream()
            .map(e -> Tag.of(e.getKey(), e.getValue()))
            .collect(Collectors.toSet()));
  }
}
