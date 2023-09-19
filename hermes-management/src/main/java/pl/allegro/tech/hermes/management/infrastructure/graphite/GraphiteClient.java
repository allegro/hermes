package pl.allegro.tech.hermes.management.infrastructure.graphite;

import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

public interface GraphiteClient {

    MonitoringMetricsContainer readMetrics(String... metricPaths);
}
