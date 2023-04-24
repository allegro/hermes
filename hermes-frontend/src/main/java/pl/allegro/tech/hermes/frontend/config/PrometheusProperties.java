package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "frontend.prometheus")
public class PrometheusProperties {

    private boolean enabled = false;
    private Duration step = Duration.ofMinutes(1);
    private boolean descriptions = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getStep() {
        return this.step;
    }

    public void setStep(Duration step) {
        this.step = step;
    }

    public boolean isDescriptions() {
        return this.descriptions;
    }

    public void setDescriptions(boolean descriptions) {
        this.descriptions = descriptions;
    }
}
