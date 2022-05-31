package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.batch")
public class BatchProperties {

    private int poolableSize = 1024;

    private int maxPoolSize = 64 * 1024 * 1024;

    private int connectionTimeout = 500;

    private int connectionRequestTimeout = 500;

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

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }
}
