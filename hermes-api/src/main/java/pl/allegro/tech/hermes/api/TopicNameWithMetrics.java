package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
}
