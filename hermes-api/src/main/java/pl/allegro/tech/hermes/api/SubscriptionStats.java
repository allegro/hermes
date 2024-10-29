package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class SubscriptionStats {
  private final long subscriptionCount;
  private final long trackingEnabledSubscriptionCount;
  private final long avroSubscriptionCount;

  @JsonCreator
  public SubscriptionStats(
      @JsonProperty("subscriptionCount") long subscriptionCount,
      @JsonProperty("trackingEnabledSubscriptionCount") long trackingEnabledSubscriptionCount,
      @JsonProperty("avroSubscriptionCount") long avroSubscriptionCount) {
    this.subscriptionCount = subscriptionCount;
    this.trackingEnabledSubscriptionCount = trackingEnabledSubscriptionCount;
    this.avroSubscriptionCount = avroSubscriptionCount;
  }

  public long getSubscriptionCount() {
    return subscriptionCount;
  }

  public long getTrackingEnabledSubscriptionCount() {
    return trackingEnabledSubscriptionCount;
  }

  public long getAvroSubscriptionCount() {
    return avroSubscriptionCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubscriptionStats that = (SubscriptionStats) o;
    return subscriptionCount == that.subscriptionCount
        && trackingEnabledSubscriptionCount == that.trackingEnabledSubscriptionCount
        && avroSubscriptionCount == that.avroSubscriptionCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(subscriptionCount, trackingEnabledSubscriptionCount, avroSubscriptionCount);
  }

  @Override
  public String toString() {
    return "SubscriptionStats{"
        + "subscriptionCount="
        + subscriptionCount
        + ", trackingEnabledSubscriptionCount="
        + trackingEnabledSubscriptionCount
        + ", avroSubscriptionCount="
        + avroSubscriptionCount
        + '}';
  }
}
