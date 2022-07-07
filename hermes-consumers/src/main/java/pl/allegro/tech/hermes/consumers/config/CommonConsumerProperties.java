package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;
import pl.allegro.tech.hermes.consumers.CommonConsumerParameters;

@ConfigurationProperties(prefix = "consumer")
public class CommonConsumerProperties {

    private int threadPoolSize = 500;

    private int inflightSize = 100;

    private boolean filteringRateLimiterEnabled = true;

    private int healthCheckPort = 8000;

    private boolean filteringEnabled = true;

    private long subscriptionIdsCacheRemovedExpireAfterAccessSeconds = 60L;

    private BackgroundSupervisor backgroundSupervisor = new BackgroundSupervisor();

    private int signalProcessingIntervalMilliseconds = 5_000;

    private int signalProcessingQueueSize = 5_000;

    private boolean useTopicMessageSizeEnabled = false;

    private String clientId = new InetAddressInstanceIdResolver().resolve();

    private int undeliveredMessageLogPersistPeriodMs = 5000;

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public int getInflightSize() {
        return inflightSize;
    }

    public void setInflightSize(int inflightSize) {
        this.inflightSize = inflightSize;
    }

    public boolean isFilteringRateLimiterEnabled() {
        return filteringRateLimiterEnabled;
    }

    public void setFilteringRateLimiterEnabled(boolean filteringRateLimiterEnabled) {
        this.filteringRateLimiterEnabled = filteringRateLimiterEnabled;
    }

    public int getHealthCheckPort() {
        return healthCheckPort;
    }

    public void setHealthCheckPort(int healthCheckPort) {
        this.healthCheckPort = healthCheckPort;
    }

    public boolean isFilteringEnabled() {
        return filteringEnabled;
    }

    public void setFilteringEnabled(boolean filteringEnabled) {
        this.filteringEnabled = filteringEnabled;
    }

    public long getSubscriptionIdsCacheRemovedExpireAfterAccessSeconds() {
        return subscriptionIdsCacheRemovedExpireAfterAccessSeconds;
    }

    public void setSubscriptionIdsCacheRemovedExpireAfterAccessSeconds(long subscriptionIdsCacheRemovedExpireAfterAccessSeconds) {
        this.subscriptionIdsCacheRemovedExpireAfterAccessSeconds = subscriptionIdsCacheRemovedExpireAfterAccessSeconds;
    }

    public BackgroundSupervisor getBackgroundSupervisor() {
        return backgroundSupervisor;
    }

    public void setBackgroundSupervisor(BackgroundSupervisor backgroundSupervisor) {
        this.backgroundSupervisor = backgroundSupervisor;
    }

    public int getSignalProcessingIntervalMilliseconds() {
        return signalProcessingIntervalMilliseconds;
    }

    public void setSignalProcessingIntervalMilliseconds(int signalProcessingIntervalMilliseconds) {
        this.signalProcessingIntervalMilliseconds = signalProcessingIntervalMilliseconds;
    }

    public int getSignalProcessingQueueSize() {
        return signalProcessingQueueSize;
    }

    public void setSignalProcessingQueueSize(int signalProcessingQueueSize) {
        this.signalProcessingQueueSize = signalProcessingQueueSize;
    }

    public boolean isUseTopicMessageSizeEnabled() {
        return useTopicMessageSizeEnabled;
    }

    public void setUseTopicMessageSizeEnabled(boolean useTopicMessageSizeEnabled) {
        this.useTopicMessageSizeEnabled = useTopicMessageSizeEnabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getUndeliveredMessageLogPersistPeriodMs() {
        return undeliveredMessageLogPersistPeriodMs;
    }

    public void setUndeliveredMessageLogPersistPeriodMs(int undeliveredMessageLogPersistPeriodMs) {
        this.undeliveredMessageLogPersistPeriodMs = undeliveredMessageLogPersistPeriodMs;
    }

    public static final class BackgroundSupervisor {

        private int interval = 20_000;

        private int unhealthyAfter = 600_000;

        private int killAfter = 300_000;

        public int getInterval() {
            return interval;
        }

        public void setInterval(int interval) {
            this.interval = interval;
        }

        public int getUnhealthyAfter() {
            return unhealthyAfter;
        }

        public void setUnhealthyAfter(int unhealthyAfter) {
            this.unhealthyAfter = unhealthyAfter;
        }

        public int getKillAfter() {
            return killAfter;
        }

        public void setKillAfter(int killAfter) {
            this.killAfter = killAfter;
        }
    }

    public CommonConsumerParameters toCommonConsumerParameters() {
        return new CommonConsumerParameters(
                this.threadPoolSize,
                this.inflightSize,
                this.filteringRateLimiterEnabled,
                this.filteringEnabled,
                this.backgroundSupervisor.interval,
                this.backgroundSupervisor.unhealthyAfter,
                this.backgroundSupervisor.killAfter,
                this.signalProcessingIntervalMilliseconds,
                this.signalProcessingQueueSize,
                this.useTopicMessageSizeEnabled,
                this.clientId
        );
    }
}
