package pl.allegro.tech.hermes.api;

import static pl.allegro.tech.hermes.api.MetricDecimalValue.of;

import java.util.Objects;

public class TopicMetrics {
  private long published;
  private long volume;
  private MetricDecimalValue rate = of("0.0");
  private MetricDecimalValue deliveryRate = of("0.0");
  private int subscriptions;
  private MetricDecimalValue throughput = of("0.0");

  public long getPublished() {
    return published;
  }

  public long getVolume() {
    return volume;
  }

  public MetricDecimalValue getRate() {
    return rate;
  }

  public MetricDecimalValue getDeliveryRate() {
    return deliveryRate;
  }

  public int getSubscriptions() {
    return subscriptions;
  }

  public MetricDecimalValue getThroughput() {
    return throughput;
  }

  @Override
  public int hashCode() {
    return Objects.hash(published, rate, deliveryRate, subscriptions, throughput, volume);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final TopicMetrics other = (TopicMetrics) obj;
    return Objects.equals(this.published, other.published)
        && Objects.equals(this.rate, other.rate)
        && Objects.equals(this.deliveryRate, other.deliveryRate)
        && Objects.equals(this.subscriptions, other.subscriptions)
        && Objects.equals(this.throughput, other.throughput)
        && Objects.equals(this.volume, other.volume);
  }

  public static TopicMetrics unavailable() {
    return Builder.topicMetrics()
        .withRate(MetricDecimalValue.unavailable())
        .withDeliveryRate(MetricDecimalValue.unavailable())
        .withPublished(0)
        .withVolume(0)
        .withSubscriptions(0)
        .withThroughput(MetricDecimalValue.unavailable())
        .build();
  }

  public static class Builder {
    private final TopicMetrics topicMetrics;

    public Builder() {
      topicMetrics = new TopicMetrics();
    }

    public Builder withPublished(long published) {
      topicMetrics.published = published;
      return this;
    }

    public Builder withRate(MetricDecimalValue rate) {
      topicMetrics.rate = rate;
      return this;
    }

    public Builder withDeliveryRate(MetricDecimalValue deliveryRate) {
      topicMetrics.deliveryRate = deliveryRate;
      return this;
    }

    public Builder withSubscriptions(int subscriptions) {
      topicMetrics.subscriptions = subscriptions;
      return this;
    }

    public Builder withThroughput(MetricDecimalValue throughput) {
      topicMetrics.throughput = throughput;
      return this;
    }

    public static Builder topicMetrics() {
      return new Builder();
    }

    public TopicMetrics build() {
      return topicMetrics;
    }

    public Builder withVolume(long volume) {
      topicMetrics.volume = volume;
      return this;
    }
  }
}
