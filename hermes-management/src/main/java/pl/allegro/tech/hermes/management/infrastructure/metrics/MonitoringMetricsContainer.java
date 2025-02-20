package pl.allegro.tech.hermes.management.infrastructure.metrics;

import java.util.HashMap;
import java.util.Map;
import pl.allegro.tech.hermes.api.MetricDecimalValue;

public class MonitoringMetricsContainer {

  private static final MetricDecimalValue DEFAULT_VALUE = MetricDecimalValue.of("0.0");

  private final Map<String, MetricDecimalValue> metrics;
  private final boolean isAvailable;

  private MonitoringMetricsContainer(boolean isAvailable, Map<String, MetricDecimalValue> metrics) {
    this.metrics = metrics;
    this.isAvailable = isAvailable;
  }

  public static MonitoringMetricsContainer createEmpty() {
    return new MonitoringMetricsContainer(true, new HashMap<>());
  }

  public static MonitoringMetricsContainer initialized(Map<String, MetricDecimalValue> metrics) {
    return new MonitoringMetricsContainer(true, metrics);
  }

  public static MonitoringMetricsContainer unavailable() {
    return new MonitoringMetricsContainer(false, new HashMap<>());
  }

  public MonitoringMetricsContainer addMetricValue(String query, MetricDecimalValue value) {
    if (!isAvailable) {
      throw new IllegalStateException("Adding value to unavailable metrics container");
    }
    this.metrics.put(query, value);
    return this;
  }

  public MetricDecimalValue metricValue(String query) {
    if (!isAvailable) {
      return MetricDecimalValue.unavailable();
    }
    return metrics.getOrDefault(query, DEFAULT_VALUE);
  }

  public boolean hasUnavailableMetrics() {
    return !isAvailable || metrics.entrySet().stream().anyMatch(e -> !e.getValue().isAvailable());
  }
}
