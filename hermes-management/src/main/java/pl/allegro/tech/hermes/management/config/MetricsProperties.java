package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "metrics")
public class MetricsProperties {

    private String graphiteHttpUri = "http://localhost";

    private String prefix = "stats.tech.hermes";
    
    public String getGraphiteHttpUri() {
        return graphiteHttpUri;
    }

    public void setGraphiteHttpUri(String graphiteHttpUri) {
        this.graphiteHttpUri = graphiteHttpUri;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
