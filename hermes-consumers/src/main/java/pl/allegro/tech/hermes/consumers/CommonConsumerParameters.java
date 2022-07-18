package pl.allegro.tech.hermes.consumers;

import java.time.Duration;

public class CommonConsumerParameters {

    private final int threadPoolSize;
    private final int inflightSize;
    private final boolean filteringRateLimiterEnabled;
    private final boolean filteringEnabled;
    private final Duration backgroundSupervisorInterval;
    private final Duration backgroundSupervisorUnhealthyAfter;
    private final Duration backgroundSupervisorKillAfter;
    private final Duration signalProcessingInterval;
    private final int signalProcessingQueueSize;
    private final boolean useTopicMessageSizeEnabled;
    private final String clientId;

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public int getInflightSize() {
        return inflightSize;
    }

    public boolean isFilteringRateLimiterEnabled() {
        return filteringRateLimiterEnabled;
    }

    public boolean isFilteringEnabled() {
        return filteringEnabled;
    }

    public Duration getBackgroundSupervisorInterval() {
        return backgroundSupervisorInterval;
    }

    public Duration getBackgroundSupervisorUnhealthyAfter() {
        return backgroundSupervisorUnhealthyAfter;
    }

    public Duration getBackgroundSupervisorKillAfter() {
        return backgroundSupervisorKillAfter;
    }

    public Duration getSignalProcessingInterval() {
        return signalProcessingInterval;
    }

    public int getSignalProcessingQueueSize() {
        return signalProcessingQueueSize;
    }

    public boolean isUseTopicMessageSizeEnabled() {
        return useTopicMessageSizeEnabled;
    }

    public String getClientId() {
        return clientId;
    }

    public CommonConsumerParameters(
            int threadPoolSize,
            int inflightSize,
            boolean filteringRateLimiterEnabled,
            boolean filteringEnabled,
            Duration backgroundSupervisorInterval,
            Duration backgroundSupervisorUnhealthyAfter,
            Duration backgroundSupervisorKillAfter,
            Duration signalProcessingInterval,
            int signalProcessingQueueSize,
            boolean useTopicMessageSizeEnabled,
            String clientId) {
        this.threadPoolSize = threadPoolSize;
        this.inflightSize = inflightSize;
        this.filteringRateLimiterEnabled = filteringRateLimiterEnabled;
        this.filteringEnabled = filteringEnabled;
        this.backgroundSupervisorInterval = backgroundSupervisorInterval;
        this.backgroundSupervisorUnhealthyAfter = backgroundSupervisorUnhealthyAfter;
        this.backgroundSupervisorKillAfter = backgroundSupervisorKillAfter;
        this.signalProcessingInterval = signalProcessingInterval;
        this.signalProcessingQueueSize = signalProcessingQueueSize;
        this.useTopicMessageSizeEnabled = useTopicMessageSizeEnabled;
        this.clientId = clientId;
    }
}
