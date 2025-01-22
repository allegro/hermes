package pl.allegro.tech.hermes.consumers.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.google.pubsub.sender")
public class GooglePubSubSenderProperties {

  private int corePoolSize = 4;

  private Duration totalTimeout = Duration.ofMillis(600_000);

  private long batchingRequestBytesThreshold = 1024L;

  private long batchingMessageCountBytesSize = 1L;

  private Duration batchingPublishDelayThreshold = Duration.ofMillis(1);

  private String transportChannelProviderAddress = "integration";

  public int getCorePoolSize() {
    return corePoolSize;
  }

  public void setCorePoolSize(int corePoolSize) {
    this.corePoolSize = corePoolSize;
  }

  public Duration getTotalTimeout() {
    return totalTimeout;
  }

  public void setTotalTimeout(Duration totalTimeout) {
    this.totalTimeout = totalTimeout;
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

  public Duration getBatchingPublishDelayThreshold() {
    return batchingPublishDelayThreshold;
  }

  public void setBatchingPublishDelayThreshold(Duration batchingPublishDelayThreshold) {
    this.batchingPublishDelayThreshold = batchingPublishDelayThreshold;
  }

  public String getTransportChannelProviderAddress() {
    return transportChannelProviderAddress;
  }

  public void setTransportChannelProviderAddress(String transportChannelProviderAddress) {
    this.transportChannelProviderAddress = transportChannelProviderAddress;
  }
}
