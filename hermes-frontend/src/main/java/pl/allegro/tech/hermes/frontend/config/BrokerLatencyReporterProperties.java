package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "frontend.broker-latency-reporter")
public class BrokerLatencyReporterProperties {
    private boolean enabled;
    private Duration slowResponseLoggingThreshold = Duration.ofMillis(100);


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getSlowResponseLoggingThreshold() {
        return slowResponseLoggingThreshold;
    }

    public void setSlowResponseLoggingThreshold(Duration slowResponseLoggingThreshold) {
        this.slowResponseLoggingThreshold = slowResponseLoggingThreshold;
    }
}
