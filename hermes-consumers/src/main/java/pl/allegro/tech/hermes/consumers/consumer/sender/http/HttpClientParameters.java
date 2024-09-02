package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.time.Duration;

public interface HttpClientParameters {

  int getThreadPoolSize();

  boolean isThreadPoolMonitoringEnabled();

  boolean isFollowRedirectsEnabled();

  Duration getIdleTimeout();

  int getMaxRequestsQueuedPerDestination();

  Duration getConnectionTimeout();
}
