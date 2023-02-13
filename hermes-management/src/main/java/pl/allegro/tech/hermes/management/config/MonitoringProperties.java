package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monitoring-consumer-groups")
public class MonitoringProperties {

    private Boolean enabled = false;
    private Integer numberOfThreads;
    private Integer secondsBetweenScans;

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(Integer numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public Integer getSecondsBetweenScans() {
        return secondsBetweenScans;
    }

    public void setSecondsBetweenScans(Integer secondsBetweenScans) {
        this.secondsBetweenScans = secondsBetweenScans;
    }
}