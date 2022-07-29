package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "consumer.batch")
public class BatchProperties {

    private int poolableSize = 1024;

    private int maxPoolSize = 64 * 1024 * 1024;

    private Duration connectionTimeout = Duration.ofMillis(500);

    private Duration connectionRequestTimeout = Duration.ofMillis(500);

    public int getPoolableSize() {
        return poolableSize;
    }

    public void setPoolableSize(int poolableSize) {
        this.poolableSize = poolableSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Duration getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(Duration connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }
}
