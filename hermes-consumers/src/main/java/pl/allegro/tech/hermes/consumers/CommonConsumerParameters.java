package pl.allegro.tech.hermes.consumers;

public class CommonConsumerParameters {

    private final int threadPoolSize;
    private final int inflightSize;
    private final boolean filteringRateLimiterEnabled;
    private final boolean filteringEnabled;
    private final int backgroundSupervisorInterval;
    private final int backgroundSupervisorUnhealthyAfter;
    private final int backgroundSupervisorKillAfter;
    private final int signalProcessingIntervalMilliseconds;
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

    public int getBackgroundSupervisorInterval() {
        return backgroundSupervisorInterval;
    }

    public int getBackgroundSupervisorUnhealthyAfter() {
        return backgroundSupervisorUnhealthyAfter;
    }

    public int getBackgroundSupervisorKillAfter() {
        return backgroundSupervisorKillAfter;
    }

    public int getSignalProcessingIntervalMilliseconds() {
        return signalProcessingIntervalMilliseconds;
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
            int backgroundSupervisorInterval,
            int backgroundSupervisorUnhealthyAfter,
            int backgroundSupervisorKillAfter,
            int signalProcessingIntervalMilliseconds,
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
        this.signalProcessingIntervalMilliseconds = signalProcessingIntervalMilliseconds;
        this.signalProcessingQueueSize = signalProcessingQueueSize;
        this.useTopicMessageSizeEnabled = useTopicMessageSizeEnabled;
        this.clientId = clientId;
    }
}
