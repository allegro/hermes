package pl.allegro.tech.hermes.management.infrastructure.metrics;

import java.util.HashMap;
import java.util.Map;
import pl.allegro.tech.hermes.api.MetricDecimalValue;
import pl.allegro.tech.hermes.api.MetricHistogramValue;
import pl.allegro.tech.hermes.api.MetricUnavailable;
import pl.allegro.tech.hermes.api.MetricValue;

public class MonitoringMetricsContainer {

  private static final MetricDecimalValue DEFAULT_VALUE = MetricDecimalValue.of("0.0");
  private static final MetricHistogramValue DEFAULT_HISTOGRAM_VALUE =
      MetricHistogramValue.defaultValue();

  private final Map<String, MetricValue> metrics;
  private final boolean isAvailable;

  public MonitoringMetricsContainer(boolean isAvailable, Map<String, MetricValue> metrics) {
    this.isAvailable = isAvailable;
    this.metrics = metrics;
  }

  public static MonitoringMetricsContainer createEmpty() {
    return new MonitoringMetricsContainer(true, new HashMap<>());
  }

  public static MonitoringMetricsContainer initialized(Map<String, MetricValue> metrics) {
    return new MonitoringMetricsContainer(true, metrics);
  }

  public static MonitoringMetricsContainer unavailable() {
    return new MonitoringMetricsContainer(false, new HashMap<>());
  }

  public MonitoringMetricsContainer addMetricValue(String query, MetricValue value) {
    if (!isAvailable) {
      throw new IllegalStateException("Adding value to unavailable metrics container");
    }
    this.metrics.put(query, value);
    return this;
  }

  public MetricDecimalValue metricValue(String query) {
    return getMetricValueOfType(
        query, MetricDecimalValue.class, DEFAULT_VALUE, MetricDecimalValue.unavailable());
  }

  public MetricHistogramValue metricHistogramValue(String query) {
    return getMetricValueOfType(
        query,
        MetricHistogramValue.class,
        DEFAULT_HISTOGRAM_VALUE,
        MetricHistogramValue.unavailable());
  }

  private <T extends MetricValue> T getMetricValueOfType(
      String query, Class<T> type, T defaultValue, T unavailableValue) {
    if (!isAvailable) {
      return unavailableValue;
    }
    MetricValue metricValue = metrics.getOrDefault(query, defaultValue);
    if (metricValue.equals(MetricUnavailable.INSTANCE)) {
      return unavailableValue;
    }
    return type.isInstance(metricValue) ? type.cast(metricValue) : defaultValue;
  }

  public boolean hasUnavailableMetrics() {
    return !isAvailable || metrics.entrySet().stream().anyMatch(e -> !e.getValue().isAvailable());
  }
}
