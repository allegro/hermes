package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PersistentSubscriptionMetrics {

  private long delivered;
  private long discarded;
  private long volume;

  @JsonCreator
  public PersistentSubscriptionMetrics(
      @JsonProperty("delivered") long delivered,
      @JsonProperty("discarded") long discarded,
      @JsonProperty("volume") long volume) {
    this.delivered = delivered;
    this.discarded = discarded;
    this.volume = volume;
  }

  public long getDelivered() {
    return delivered;
  }

  public void setDelivered(long delivered) {
    this.delivered = delivered;
  }

  public long getDiscarded() {
    return discarded;
  }

  public void setDiscarded(long discarded) {
    this.discarded = discarded;
  }

  public long getVolume() {
    return volume;
  }

  public void setVolume(long volume) {
    this.volume = volume;
  }
}
