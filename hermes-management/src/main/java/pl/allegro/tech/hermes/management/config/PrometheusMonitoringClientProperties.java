package pl.allegro.tech.hermes.management.config;

public class PrometheusMonitoringClientProperties extends ExternalMonitoringClientProperties {
    private String consumersMetricsPrefix = "hermes_consumers";
    private String frontendMetricsPrefix = "hermes_frontend";

    public String getConsumersMetricsPrefix() {
        return consumersMetricsPrefix;
    }

    public void setConsumersMetricsPrefix(String consumersMetricsPrefix) {
        this.consumersMetricsPrefix = consumersMetricsPrefix;
    }

    public String getFrontendMetricsPrefix() {
        return frontendMetricsPrefix;
    }

    public void setFrontendMetricsPrefix(String frontendMetricsPrefix) {
        this.frontendMetricsPrefix = frontendMetricsPrefix;
    }
}
