package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class SubscriptionNameWithMetrics {
  private final String topicName;
  private final String name;
  private final long delivered;
  private final long discarded;
  private final long volume;
  private final MetricDecimalValue timeouts;
  private final MetricLongValue lag;
  private final MetricDecimalValue rate;
  private final MetricDecimalValue throughput;

  @JsonCreator
  public SubscriptionNameWithMetrics(
      @JsonProperty("topicName") String topicName,
      @JsonProperty("name") String name,
      @JsonProperty("delivered") long delivered,
      @JsonProperty("discarded") long discarded,
      @JsonProperty("volume") long volume,
      @JsonProperty("timeouts") MetricDecimalValue timeouts,
      @JsonProperty("lag") MetricLongValue lag,
      @JsonProperty("rate") MetricDecimalValue rate,
      @JsonProperty("throughput") MetricDecimalValue throughput) {
    this.topicName = topicName;
    this.name = name;
    this.delivered = delivered;
    this.discarded = discarded;
    this.volume = volume;
    this.timeouts = timeouts;
    this.lag = lag;
    this.rate = rate;
    this.throughput = throughput;
  }

  public static SubscriptionNameWithMetrics from(
      SubscriptionMetrics metrics, String name, String topicQualifiedName) {
    return new SubscriptionNameWithMetrics(
        topicQualifiedName,
        name,
        metrics.getDelivered(),
        metrics.getDiscarded(),
        metrics.getVolume(),
        metrics.getTimeouts(),
        metrics.getLag(),
        metrics.getRate(),
        metrics.getThroughput());
  }

  public String getTopicName() {
    return topicName;
  }

  public String getName() {
    return name;
  }

  public long getDelivered() {
    return delivered;
  }

  public long getDiscarded() {
    return discarded;
  }

  public MetricDecimalValue getTimeouts() {
    return timeouts;
  }

  public MetricLongValue getLag() {
    return lag;
  }

  public MetricDecimalValue getRate() {
    return rate;
  }

  public MetricDecimalValue getThroughput() {
    return throughput;
  }

  public long getVolume() {
    return volume;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SubscriptionNameWithMetrics that = (SubscriptionNameWithMetrics) o;

    return Objects.equals(this.topicName, that.topicName)
        && Objects.equals(this.name, that.name)
        && Objects.equals(this.delivered, that.delivered)
        && Objects.equals(this.discarded, that.discarded)
        && Objects.equals(this.timeouts, that.timeouts)
        && Objects.equals(this.lag, that.lag)
        && Objects.equals(this.rate, that.rate)
        && Objects.equals(this.throughput, that.throughput)
        && Objects.equals(this.volume, that.volume);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.topicName,
        this.name,
        this.delivered,
        this.discarded,
        this.timeouts,
        this.lag,
        this.rate,
        this.throughput,
        this.volume);
  }
}
