package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientParameters;

import java.time.Duration;

@ConfigurationProperties(prefix = "consumer.http.client")
public class HttpClientProperties {

    private boolean connectionPoolMonitoringEnabled = false;

    private boolean requestQueueMonitoringEnabled = true;

    private int threadPoolSize = 30;

    private boolean threadPoolMonitoringEnabled = false;

    private boolean followRedirectsEnabled = false;

    private int maxConnectionsPerDestination = 100;

    private Duration idleTimeout = Duration.ofMillis(0);

    private int maxRequestsQueuedPerDestination = 100;

    public boolean isConnectionPoolMonitoringEnabled() {
        return connectionPoolMonitoringEnabled;
    }

    public void setConnectionPoolMonitoringEnabled(boolean connectionPoolMonitoringEnabled) {
        this.connectionPoolMonitoringEnabled = connectionPoolMonitoringEnabled;
    }

    public boolean isRequestQueueMonitoringEnabled() {
        return requestQueueMonitoringEnabled;
    }

    public void setRequestQueueMonitoringEnabled(boolean requestQueueMonitoringEnabled) {
        this.requestQueueMonitoringEnabled = requestQueueMonitoringEnabled;
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

    public boolean isFollowRedirectsEnabled() {
        return followRedirectsEnabled;
    }

    public void setFollowRedirectsEnabled(boolean followRedirectsEnabled) {
        this.followRedirectsEnabled = followRedirectsEnabled;
    }

    public int getMaxConnectionsPerDestination() {
        return maxConnectionsPerDestination;
    }

    public void setMaxConnectionsPerDestination(int maxConnectionsPerDestination) {
        this.maxConnectionsPerDestination = maxConnectionsPerDestination;
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

    public HttpClientParameters toHttpClientParameters() {
        return new HttpClientParameters(
                this.threadPoolSize,
                this.threadPoolMonitoringEnabled,
                this.followRedirectsEnabled,
                this.maxConnectionsPerDestination,
                this.idleTimeout,
                this.maxRequestsQueuedPerDestination
        );
    }
}
