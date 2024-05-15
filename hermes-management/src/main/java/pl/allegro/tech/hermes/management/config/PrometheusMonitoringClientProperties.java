package pl.allegro.tech.hermes.management.config;

public class PrometheusMonitoringClientProperties extends ExternalMonitoringClientProperties {
    private String consumersMetricsPrefix = "hermes_consumers";
    private String frontendMetricsPrefix = "hermes_frontend";
    private String additionalFilters = "";
    private String bearerToken = "";

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

    public String getAdditionalFilters() {
        return additionalFilters;
    }

    public void setAdditionalFilters(String additionalFilters) {
        this.additionalFilters = additionalFilters;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }
}
