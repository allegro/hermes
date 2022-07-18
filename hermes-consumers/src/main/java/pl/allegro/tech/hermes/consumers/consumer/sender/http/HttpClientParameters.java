package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.time.Duration;

public interface HttpClientParameters {

    int getThreadPoolSize();

    boolean isThreadPoolMonitoringEnabled();

    boolean isFollowRedirectsEnabled();

    int getMaxConnectionsPerDestination();

    Duration getIdleTimeout();

    int getMaxRequestsQueuedPerDestination();
}
