package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.time.Duration;

public interface Http2ClientParameters {

    int getThreadPoolSize();

    boolean isThreadPoolMonitoringEnabled();

    Duration getIdleTimeout();

    int getMaxRequestsQueuedPerDestination();
}
