package pl.allegro.tech.hermes.consumers.consumer.sender.http;

public class HttpClientParameters {

    private final int threadPoolSize;

    private final boolean threadPoolMonitoringEnabled;

    private final boolean followRedirectsEnabled;

    private final int maxConnectionsPerDestination;

    private final int idleTimeout;

    private final int maxRequestsQueuedPerDestination;

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public boolean isThreadPoolMonitoringEnabled() {
        return threadPoolMonitoringEnabled;
    }

    public boolean isFollowRedirectsEnabled() {
        return followRedirectsEnabled;
    }

    public int getMaxConnectionsPerDestination() {
        return maxConnectionsPerDestination;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public int getMaxRequestsQueuedPerDestination() {
        return maxRequestsQueuedPerDestination;
    }

    public HttpClientParameters(int threadPoolSize,
                                boolean threadPoolMonitoringEnabled,
                                boolean followRedirectsEnabled,
                                int maxConnectionsPerDestination,
                                int idleTimeout,
                                int maxRequestsQueuedPerDestination) {
        this.threadPoolSize = threadPoolSize;
        this.threadPoolMonitoringEnabled = threadPoolMonitoringEnabled;
        this.followRedirectsEnabled = followRedirectsEnabled;
        this.maxConnectionsPerDestination = maxConnectionsPerDestination;
        this.idleTimeout = idleTimeout;
        this.maxRequestsQueuedPerDestination = maxRequestsQueuedPerDestination;
    }
}
