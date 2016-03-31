package pl.allegro.tech.hermes.management.infrastructure.graphite;

public interface GraphiteClient {
    GraphiteMetrics readMetrics(String... metricPaths);
}
