package pl.allegro.tech.hermes.management.infrastructure.graphite;

import pl.allegro.tech.hermes.api.MetricDecimalValue;

import java.util.HashMap;
import java.util.Map;

public class GraphiteMetrics {

    private static final MetricDecimalValue DEFAULT_VALUE = MetricDecimalValue.of("0.0");

    private final Map<String, MetricDecimalValue> metrics = new HashMap<>();

    public GraphiteMetrics() {
    }

    public GraphiteMetrics(Map<String, MetricDecimalValue> metrics) {
        this.metrics.putAll(metrics);
    }

    public static GraphiteMetrics unavailable(String... metrics) {
        GraphiteMetrics graphiteMetrics = new GraphiteMetrics();
        for (String metric : metrics) {
            graphiteMetrics.addMetricValue(metric, MetricDecimalValue.unavailable());
        }
        return graphiteMetrics;
    }

    public GraphiteMetrics addMetricValue(String metricPath, MetricDecimalValue value) {
        this.metrics.put(metricPath, value);
        return this;
    }

    public MetricDecimalValue metricValue(String metricPath) {
        return metrics.getOrDefault(metricPath, DEFAULT_VALUE);
    }
}
