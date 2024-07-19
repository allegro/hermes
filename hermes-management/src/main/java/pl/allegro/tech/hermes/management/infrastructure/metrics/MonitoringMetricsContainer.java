package pl.allegro.tech.hermes.management.infrastructure.metrics;

import pl.allegro.tech.hermes.api.MetricDecimalValue;

import java.util.HashMap;
import java.util.Map;

public class MonitoringMetricsContainer {

    private static final MetricDecimalValue DEFAULT_VALUE = MetricDecimalValue.of("0.0");

    private final Map<MetricsQuery, MetricDecimalValue> metrics;

    private MonitoringMetricsContainer(Map<MetricsQuery, MetricDecimalValue> metrics) {
        this.metrics = metrics;
    }

    public static MonitoringMetricsContainer createEmpty() {
        return new MonitoringMetricsContainer(new HashMap<>());
    }

    public static MonitoringMetricsContainer initialized(Map<MetricsQuery, MetricDecimalValue> metrics) {
        return new MonitoringMetricsContainer(metrics);
    }

    public MonitoringMetricsContainer addMetricValue(MetricsQuery query, MetricDecimalValue value) {
        this.metrics.put(query, value);
        return this;
    }

    public MetricDecimalValue metricValue(MetricsQuery query) {
        return metrics.getOrDefault(query, DEFAULT_VALUE);
    }

    public boolean containsUnavailable() {
        return metrics.entrySet().stream().anyMatch(e -> !e.getValue().isAvailable());
    }
}
