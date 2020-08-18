package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class TopicNameWithMetrics {

    private final long published;
    private final MetricDecimalValue rate;
    private final MetricDecimalValue deliveryRate;
    private final int subscriptions;
    private final MetricDecimalValue throughput;
    private final long volume;

    private final TopicName topicName;

    @JsonCreator
    public TopicNameWithMetrics(
            @JsonProperty("published") long published,
            @JsonProperty("rate") MetricDecimalValue rate,
            @JsonProperty("deliveryRate") MetricDecimalValue deliveryRate,
            @JsonProperty("subscriptions") int subscriptions,
            @JsonProperty("throughput") MetricDecimalValue throughput,
            @JsonProperty("volume") long volume,
            @JsonProperty("name") String qualifiedName
    ) {
        this.published = published;
        this.rate = rate;
        this.deliveryRate = deliveryRate;
        this.subscriptions = subscriptions;
        this.throughput = throughput;
        this.volume = volume;
        this.topicName = TopicName.fromQualifiedName(qualifiedName);
    }

    public static TopicNameWithMetrics from(TopicMetrics metrics, String qualifiedName) {
        return new TopicNameWithMetrics(
                metrics.getPublished(),
                metrics.getRate(),
                metrics.getDeliveryRate(),
                metrics.getSubscriptions(),
                metrics.getThroughput(),
                metrics.getVolume(),
                qualifiedName
        );
    }

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

    @JsonProperty("name")
    public String getQualifiedName() {
        return topicName.qualifiedName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TopicNameWithMetrics that = (TopicNameWithMetrics) o;

        return Objects.equals(this.published, that.published)
                && Objects.equals(this.rate, that.rate)
                && Objects.equals(this.deliveryRate, that.deliveryRate)
                && Objects.equals(this.subscriptions, that.subscriptions)
                && Objects.equals(this.throughput, that.throughput)
                && Objects.equals(this.volume, that.volume)
                && Objects.equals(this.topicName, that.topicName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(published, rate, deliveryRate, subscriptions, throughput, topicName, volume);
    }
}
