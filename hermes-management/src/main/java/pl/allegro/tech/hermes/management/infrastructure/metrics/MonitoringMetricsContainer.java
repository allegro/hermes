package pl.allegro.tech.hermes.management.infrastructure.metrics;

import pl.allegro.tech.hermes.api.MetricDecimalValue;

import java.util.HashMap;
import java.util.Map;

public class MonitoringMetricsContainer {

    private static final MetricDecimalValue DEFAULT_VALUE = MetricDecimalValue.of("0.0");

    private final Map<String, MetricDecimalValue> metrics = new HashMap<>();

    public MonitoringMetricsContainer() {
    }

    public MonitoringMetricsContainer(Map<String, MetricDecimalValue> metrics) {
        this.metrics.putAll(metrics);
    }

    public static MonitoringMetricsContainer unavailable(String... metrics) {
        MonitoringMetricsContainer metricsContainer = new MonitoringMetricsContainer();
        for (String metric : metrics) {
            metricsContainer.addMetricValue(metric, MetricDecimalValue.unavailable());
        }
        return metricsContainer;
    }

    public MonitoringMetricsContainer addMetricValue(String metricPath, MetricDecimalValue value) {
        this.metrics.put(metricPath, value);
        return this;
    }

    public MetricDecimalValue metricValue(String metricPath) {
        return metrics.getOrDefault(metricPath, DEFAULT_VALUE);
    }
}
