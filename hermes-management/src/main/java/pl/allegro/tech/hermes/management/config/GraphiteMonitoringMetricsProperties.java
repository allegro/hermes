package pl.allegro.tech.hermes.management.config;

public class GraphiteMonitoringMetricsProperties extends ExternalMonitoringClientProperties{

    private String prefix = "stats.tech.hermes";

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
