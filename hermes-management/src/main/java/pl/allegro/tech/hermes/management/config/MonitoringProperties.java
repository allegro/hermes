package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "monitoring-consumer-groups")
public class MonitoringProperties {

    private boolean enabled = true;
    private int numberOfThreads;
    private Duration scanEvery;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public Duration getScanEvery() {
        return scanEvery;
    }

    public void setScanEvery(Duration scanEvery) {
        this.scanEvery = scanEvery;
    }
}