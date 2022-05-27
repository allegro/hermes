package pl.allegro.tech.hermes.consumers.consumer.sender.http;

public class Http2ClientParameters {

    private final int threadPoolSize;

    private final boolean threadPoolMonitoringEnabled;

    private final int idleTimeout;

    private final int maxRequestsQueuedPerDestination;

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public boolean isThreadPoolMonitoringEnabled() {
        return threadPoolMonitoringEnabled;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public int getMaxRequestsQueuedPerDestination() {
        return maxRequestsQueuedPerDestination;
    }

    public Http2ClientParameters(int threadPoolSize, boolean threadPoolMonitoringEnabled, int idleTimeout, int maxRequestsQueuedPerDestination) {
        this.threadPoolSize = threadPoolSize;
        this.threadPoolMonitoringEnabled = threadPoolMonitoringEnabled;
        this.idleTimeout = idleTimeout;
        this.maxRequestsQueuedPerDestination = maxRequestsQueuedPerDestination;
    }
}
