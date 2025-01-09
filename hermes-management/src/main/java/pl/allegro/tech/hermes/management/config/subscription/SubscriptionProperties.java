package pl.allegro.tech.hermes.management.config.subscription;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("subscription")
public class SubscriptionProperties {

  private List<String> additionalEndpointProtocols = new ArrayList<>();

  private List<SubscriberProperties> subscribersWithAccessToAnyTopic = new ArrayList<>();

  private int intervalBetweenCheckinIfOffsetsMovedInMillis = 50;

  private int offsetsMovedTimeoutInSeconds = 30;

  private boolean createConsumerGroupManuallyEnabled = true;

  public List<String> getAdditionalEndpointProtocols() {
    return additionalEndpointProtocols;
  }

  public void setAdditionalEndpointProtocols(List<String> additionalEndpointProtocols) {
    this.additionalEndpointProtocols = additionalEndpointProtocols;
  }

  public int getIntervalBetweenCheckinIfOffsetsMovedInMillis() {
    return intervalBetweenCheckinIfOffsetsMovedInMillis;
  }

  public void setIntervalBetweenCheckinIfOffsetsMovedInMillis(
      int intervalBetweenCheckinIfOffsetsMovedInMillis) {
    this.intervalBetweenCheckinIfOffsetsMovedInMillis =
        intervalBetweenCheckinIfOffsetsMovedInMillis;
  }

  public int getOffsetsMovedTimeoutInSeconds() {
    return offsetsMovedTimeoutInSeconds;
  }

  public void setOffsetsMovedTimeoutInSeconds(int offsetsMovedTimeoutInSeconds) {
    this.offsetsMovedTimeoutInSeconds = offsetsMovedTimeoutInSeconds;
  }

  public boolean isCreateConsumerGroupManuallyEnabled() {
    return createConsumerGroupManuallyEnabled;
  }

  public void setCreateConsumerGroupManuallyEnabled(boolean createConsumerGroupManuallyEnabled) {
    this.createConsumerGroupManuallyEnabled = createConsumerGroupManuallyEnabled;
  }

  public List<SubscriberProperties> getSubscribersWithAccessToAnyTopic() {
    return subscribersWithAccessToAnyTopic;
  }

  public void setSubscribersWithAccessToAnyTopic(
      List<SubscriberProperties> subscribersWithAccessToAnyTopic) {
    this.subscribersWithAccessToAnyTopic = subscribersWithAccessToAnyTopic;
  }

  public static class SubscriberProperties {
    private String ownerSource;
    private String ownerId;
    private List<String> protocols = new ArrayList<>();

    public String getOwnerSource() {
      return ownerSource;
    }

    public void setOwnerSource(String ownerSource) {
      this.ownerSource = ownerSource;
    }

    public String getOwnerId() {
      return ownerId;
    }

    public void setOwnerId(String ownerId) {
      this.ownerId = ownerId;
    }

    public List<String> getProtocols() {
      return protocols;
    }

    public void setProtocols(List<String> protocols) {
      this.protocols = protocols;
    }
  }
}
