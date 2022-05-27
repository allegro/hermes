package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.pubsub.sender")
public class GooglePubSubSenderProperties {

    private int corePoolSize = 4;

    private long totalTimeoutsMilliseconds = 600_000L;

    private long batchingRequestBytesThreshold = 1024L;

    private long batchingMessageCountBytesSize = 1L;

    private long batchingPublishDelayThresholdMilliseconds = 1L;

    private String transportChannelProviderAddress = "integration";

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public long getTotalTimeoutsMilliseconds() {
        return totalTimeoutsMilliseconds;
    }

    public void setTotalTimeoutsMilliseconds(long totalTimeoutsMilliseconds) {
        this.totalTimeoutsMilliseconds = totalTimeoutsMilliseconds;
    }

    public long getBatchingRequestBytesThreshold() {
        return batchingRequestBytesThreshold;
    }

    public void setBatchingRequestBytesThreshold(long batchingRequestBytesThreshold) {
        this.batchingRequestBytesThreshold = batchingRequestBytesThreshold;
    }

    public long getBatchingMessageCountBytesSize() {
        return batchingMessageCountBytesSize;
    }

    public void setBatchingMessageCountBytesSize(long batchingMessageCountBytesSize) {
        this.batchingMessageCountBytesSize = batchingMessageCountBytesSize;
    }

    public long getBatchingPublishDelayThresholdMilliseconds() {
        return batchingPublishDelayThresholdMilliseconds;
    }

    public void setBatchingPublishDelayThresholdMilliseconds(long batchingPublishDelayThresholdMilliseconds) {
        this.batchingPublishDelayThresholdMilliseconds = batchingPublishDelayThresholdMilliseconds;
    }

    public String getTransportChannelProviderAddress() {
        return transportChannelProviderAddress;
    }

    public void setTransportChannelProviderAddress(String transportChannelProviderAddress) {
        this.transportChannelProviderAddress = transportChannelProviderAddress;
    }
}
