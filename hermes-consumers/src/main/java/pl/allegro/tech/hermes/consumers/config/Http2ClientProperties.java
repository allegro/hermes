package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.Http2ClientParameters;

import java.time.Duration;

@ConfigurationProperties(prefix = "consumer.http2.client")
public class Http2ClientProperties {

    private boolean enabled = true;

    private int threadPoolSize = 10;

    private boolean threadPoolMonitoringEnabled = false;

    private Duration idleTimeout = Duration.ofMillis(0);

    private int maxRequestsQueuedPerDestination = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public boolean isThreadPoolMonitoringEnabled() {
        return threadPoolMonitoringEnabled;
    }

    public void setThreadPoolMonitoringEnabled(boolean threadPoolMonitoringEnabled) {
        this.threadPoolMonitoringEnabled = threadPoolMonitoringEnabled;
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getMaxRequestsQueuedPerDestination() {
        return maxRequestsQueuedPerDestination;
    }

    public void setMaxRequestsQueuedPerDestination(int maxRequestsQueuedPerDestination) {
        this.maxRequestsQueuedPerDestination = maxRequestsQueuedPerDestination;
    }

    public Http2ClientParameters toHttp2ClientParameters() {
        return new Http2ClientParameters(
                this.threadPoolSize,
                this.threadPoolMonitoringEnabled,
                this.idleTimeout,
                this.maxRequestsQueuedPerDestination
        );
    }
}
