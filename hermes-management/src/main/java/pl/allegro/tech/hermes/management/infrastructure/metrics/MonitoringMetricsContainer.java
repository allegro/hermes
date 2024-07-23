package pl.allegro.tech.hermes.management.infrastructure.metrics;

import pl.allegro.tech.hermes.api.MetricDecimalValue;

import java.util.HashMap;
import java.util.Map;

public class MonitoringMetricsContainer {

    private static final MetricDecimalValue DEFAULT_VALUE = MetricDecimalValue.of("0.0");

    private final Map<MetricsQuery, MetricDecimalValue> metrics;
    private final boolean isAvailable;

    private MonitoringMetricsContainer(boolean isAvailable, Map<MetricsQuery, MetricDecimalValue> metrics) {
        this.metrics = metrics;
        this.isAvailable = isAvailable;
    }

    public static MonitoringMetricsContainer createEmpty() {
        return new MonitoringMetricsContainer(true, new HashMap<>());
    }

    public static MonitoringMetricsContainer initialized(Map<MetricsQuery, MetricDecimalValue> metrics) {
        return new MonitoringMetricsContainer(true, metrics);
    }

    public static MonitoringMetricsContainer unavailable() {
        return new MonitoringMetricsContainer(false, new HashMap<>());
    }

    public MonitoringMetricsContainer addMetricValue(MetricsQuery query, MetricDecimalValue value) {
        if (!isAvailable) {
            throw new IllegalStateException("Adding value to unavailable metrics container");
        }
        this.metrics.put(query, value);
        return this;
    }

    public MetricDecimalValue metricValue(MetricsQuery query) {
        if (!isAvailable) {
            return MetricDecimalValue.unavailable();
        }
        return metrics.getOrDefault(query, DEFAULT_VALUE);
    }

    public boolean hasUnavailableMetrics() {
        return !isAvailable || metrics.entrySet().stream().anyMatch(e -> !e.getValue().isAvailable());
    }
}
