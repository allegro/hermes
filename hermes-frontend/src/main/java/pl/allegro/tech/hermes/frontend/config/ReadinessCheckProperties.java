package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "frontend.readiness.check")
public class ReadinessCheckProperties {

    private boolean enabled = false;

    private Duration interval = Duration.ofSeconds(1);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getInterval() {
        return interval;
    }

    public void setIntervalSeconds(Duration intervalSeconds) {
        this.interval = intervalSeconds;
    }
}
