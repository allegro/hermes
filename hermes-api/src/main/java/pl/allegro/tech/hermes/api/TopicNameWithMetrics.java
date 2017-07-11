package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class TopicNameWithMetrics {

    private final long published;
    private final String rate;
    private final String deliveryRate;
    private final int subscriptions;
    private final String throughput;

    private final TopicName topicName;

    @JsonCreator
    public TopicNameWithMetrics(
            @JsonProperty("published") long published,
            @JsonProperty("rate") String rate,
            @JsonProperty("deliveryRate") String deliveryRate,
            @JsonProperty("subscriptions") int subscriptions,
            @JsonProperty("throughput") String throughput,
            @JsonProperty("qualifiedName") String qualifiedName
    ) {
        this.published = published;
        this.rate = rate;
        this.deliveryRate = deliveryRate;
        this.subscriptions = subscriptions;
        this.throughput = throughput;
        this.topicName = TopicName.fromQualifiedName(qualifiedName);
    }

    public static TopicNameWithMetrics from(TopicMetrics metrics, String qualifiedName) {
        return new TopicNameWithMetrics(
                metrics.getPublished(),
                metrics.getRate(),
                metrics.getDeliveryRate(),
                metrics.getSubscriptions(),
                metrics.getThroughput(),
                qualifiedName
        );
    }

    public long getPublished() {
        return published;
    }

    public String getRate() {
        return rate;
    }

    public String getDeliveryRate() {
        return deliveryRate;
    }

    public int getSubscriptions() {
        return subscriptions;
    }

    public String getThroughput() {
        return throughput;
    }

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
                && Objects.equals(this.topicName, that.topicName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(published, rate, deliveryRate, subscriptions, throughput, topicName);
    }
}
