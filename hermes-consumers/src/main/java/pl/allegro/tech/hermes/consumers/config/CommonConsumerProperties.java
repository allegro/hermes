package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;
import pl.allegro.tech.hermes.consumers.CommonConsumerParameters;

import java.time.Duration;

@ConfigurationProperties(prefix = "consumer")
public class CommonConsumerProperties {

    private int threadPoolSize = 500;

    private int inflightSize = 100;

    private boolean filteringRateLimiterEnabled = true;

    private int healthCheckPort = 8000;

    private boolean filteringEnabled = true;

    private Duration subscriptionIdsCacheRemovedExpireAfterAccess = Duration.ofSeconds(60);

    private BackgroundSupervisor backgroundSupervisor = new BackgroundSupervisor();

    private Duration signalProcessingInterval = Duration.ofMillis(5_000);

    private int signalProcessingQueueSize = 5_000;

    private boolean useTopicMessageSizeEnabled = false;

    private String clientId = new InetAddressInstanceIdResolver().resolve();

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

    public Duration getSubscriptionIdsCacheRemovedExpireAfterAccess() {
        return subscriptionIdsCacheRemovedExpireAfterAccess;
    }

    public void setSubscriptionIdsCacheRemovedExpireAfterAccess(Duration subscriptionIdsCacheRemovedExpireAfterAccess) {
        this.subscriptionIdsCacheRemovedExpireAfterAccess = subscriptionIdsCacheRemovedExpireAfterAccess;
    }

    public BackgroundSupervisor getBackgroundSupervisor() {
        return backgroundSupervisor;
    }

    public void setBackgroundSupervisor(BackgroundSupervisor backgroundSupervisor) {
        this.backgroundSupervisor = backgroundSupervisor;
    }

    public Duration getSignalProcessingInterval() {
        return signalProcessingInterval;
    }

    public void setSignalProcessingInterval(Duration signalProcessingInterval) {
        this.signalProcessingInterval = signalProcessingInterval;
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

    public static final class BackgroundSupervisor {

        private Duration interval = Duration.ofMillis(20_000);

        private Duration unhealthyAfter = Duration.ofMillis(600_000);

        private Duration killAfter = Duration.ofMillis(300_000);

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }

        public Duration getUnhealthyAfter() {
            return unhealthyAfter;
        }

        public void setUnhealthyAfter(Duration unhealthyAfter) {
            this.unhealthyAfter = unhealthyAfter;
        }

        public Duration getKillAfter() {
            return killAfter;
        }

        public void setKillAfter(Duration killAfter) {
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
                this.signalProcessingInterval,
                this.signalProcessingQueueSize,
                this.useTopicMessageSizeEnabled,
                this.clientId
        );
    }
}
