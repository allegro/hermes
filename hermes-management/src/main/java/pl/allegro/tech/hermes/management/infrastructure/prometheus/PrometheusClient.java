package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

public interface PrometheusClient {
    MonitoringMetricsContainer readMetrics(String query);
}
