package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientParameters;

import java.time.Duration;

@ConfigurationProperties(prefix = "consumer.http.client")
public class HttpClientProperties implements HttpClientParameters {

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

    @Override
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    @Override
    public boolean isThreadPoolMonitoringEnabled() {
        return threadPoolMonitoringEnabled;
    }

    public void setThreadPoolMonitoringEnabled(boolean threadPoolMonitoringEnabled) {
        this.threadPoolMonitoringEnabled = threadPoolMonitoringEnabled;
    }

    @Override
    public boolean isFollowRedirectsEnabled() {
        return followRedirectsEnabled;
    }

    public void setFollowRedirectsEnabled(boolean followRedirectsEnabled) {
        this.followRedirectsEnabled = followRedirectsEnabled;
    }

    @Override
    public int getMaxConnectionsPerDestination() {
        return maxConnectionsPerDestination;
    }

    public void setMaxConnectionsPerDestination(int maxConnectionsPerDestination) {
        this.maxConnectionsPerDestination = maxConnectionsPerDestination;
    }

    @Override
    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    @Override
    public int getMaxRequestsQueuedPerDestination() {
        return maxRequestsQueuedPerDestination;
    }

    public void setMaxRequestsQueuedPerDestination(int maxRequestsQueuedPerDestination) {
        this.maxRequestsQueuedPerDestination = maxRequestsQueuedPerDestination;
    }
}
