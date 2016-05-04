package pl.allegro.tech.hermes.management.infrastructure.graphite;

import java.util.HashMap;
import java.util.Map;

public class GraphiteMetrics {

    private static final String DEFAULT_VALUE = "0.0";

    private final Map<String, String> metrics = new HashMap<>();

    public GraphiteMetrics() {
    }

    public GraphiteMetrics(Map<String, String> metrics) {
        this.metrics.putAll(metrics);
    }

    public static GraphiteMetrics unavailable(String... metrics) {
        GraphiteMetrics graphiteMetrics = new GraphiteMetrics();
        for (String metric : metrics) {
            graphiteMetrics.addMetricValue(metric, "unavailable");
        }
        return graphiteMetrics;
    }

    public GraphiteMetrics addMetricValue(String metricPath, String value) {
        this.metrics.put(metricPath, value);
        return this;
    }

    public String metricValue(String metricPath) {
        return metrics.getOrDefault(metricPath, DEFAULT_VALUE);
    }
}
