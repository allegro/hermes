package pl.allegro.tech.hermes.consumers;

import java.time.Duration;

public interface CommonConsumerParameters {

    int getThreadPoolSize();

    int getInflightSize();

    boolean isFilteringRateLimiterEnabled();

    boolean isFilteringEnabled();

    Duration getBackgroundSupervisorInterval();

    Duration getBackgroundSupervisorUnhealthyAfter();

    Duration getBackgroundSupervisorKillAfter();

    Duration getSignalProcessingInterval();

    int getSignalProcessingQueueSize();

    boolean isUseTopicMessageSizeEnabled();

    String getClientId();
}
