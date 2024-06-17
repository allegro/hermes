package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "consumer.metrics.metric-registry")
public class MetricsProperties {

    private Duration counterExpireAfterAccess = Duration.ofHours(72);

    public Duration getCounterExpireAfterAccess() {
        return counterExpireAfterAccess;
    }

    public void setCounterExpireAfterAccess(Duration counterExpireAfterAccess) {
        this.counterExpireAfterAccess = counterExpireAfterAccess;
    }
}
