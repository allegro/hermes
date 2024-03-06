package pl.allegro.tech.hermes.integrationtests.prometheus;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class PrometheusEndpoint {
    private final Client client = ClientBuilder.newClient();
    private final int port;

    public PrometheusEndpoint(int port) {
        this.port = port;
    }

    public Optional<Double> getMetricValue(String metricPrefix, Map<String, String> labels) {
        String metricsResponse = client.target("http://127.0.0.1:" + port + "/status/prometheus").request().get(String.class);
        return Arrays.stream(metricsResponse.split("\n"))
                .filter(metricName -> metricName.startsWith(metricPrefix))
                .filter(metricName -> containsLabels(metricName, labels))
                .findFirst()
                .map(line -> line.split(" ")[1]) // metrics have format "<metric_name> <metric_value>"
                .map(Double::valueOf);
    }
    private boolean containsLabels(String metric, Map<String, String> labels) {
        for (var entry : labels.entrySet()) {
            String substr = String.format("%s=\"%s\"", entry.getKey(), entry.getValue());
            if (!metric.contains(substr)) {
                return false;
            }
        }
        return true;
    }
}
