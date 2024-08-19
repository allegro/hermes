package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "frontend.broker-latency-reporter")
public class BrokerLatencyReporterProperties {
    private boolean enabled;
    private Duration slowResponseLoggingThreshold = Duration.ofMillis(100);
    private int threadPoolSize = 8;
    private int threadPoolQueueCapacity = 1_000_000;

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public int getThreadPoolQueueCapacity() {
        return threadPoolQueueCapacity;
    }

    public void setThreadPoolQueueCapacity(int threadPoolQueueCapacity) {
        this.threadPoolQueueCapacity = threadPoolQueueCapacity;
    }

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
