package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class SubscriptionNameWithMetrics {
    private final String topicQualifiedName;
    private final String name;
    private final long delivered;
    private final long discarded;
    private final long inflight;
    private final String timeouts;
    private final long lag;
    private final String rate;
    private final String throughput;

    @JsonCreator
    public SubscriptionNameWithMetrics(
            @JsonProperty("topicQualifiedName") String topicQualifiedName,
            @JsonProperty("name") String name,
            @JsonProperty("delivered") long delivered,
            @JsonProperty("discarded") long discarded,
            @JsonProperty("inflight") long inflight,
            @JsonProperty("timeouts") String timeouts,
            @JsonProperty("lag") long lag,
            @JsonProperty("rate") String rate,
            @JsonProperty("throughput") String throughput
    ) {
        this.topicQualifiedName = topicQualifiedName;
        this.name = name;
        this.delivered = delivered;
        this.discarded = discarded;
        this.inflight = inflight;
        this.timeouts = timeouts;
        this.lag = lag;
        this.rate = rate;
        this.throughput = throughput;
    }

    public static SubscriptionNameWithMetrics from(SubscriptionMetrics metrics, String name, String topicQualifiedName) {
        return new SubscriptionNameWithMetrics(topicQualifiedName, name, metrics.getDelivered(),
                metrics.getDiscarded(), metrics.getInflight(), metrics.getTimeouts(), metrics.getLag(),
                metrics.getRate(), metrics.getThroughput());
    }

    public String getTopicQualifiedName() {
        return topicQualifiedName;
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

    public long getInflight() {
        return inflight;
    }

    public String getTimeouts() {
        return timeouts;
    }

    public long getLag() {
        return lag;
    }

    public String getRate() {
        return rate;
    }

    public String getThroughput() {
        return throughput;
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

        return Objects.equals(this.topicQualifiedName, that.topicQualifiedName)
                && Objects.equals(this.name, that.name)
                && Objects.equals(this.delivered, that.delivered)
                && Objects.equals(this.discarded, that.discarded)
                && Objects.equals(this.inflight, that.inflight)
                && Objects.equals(this.timeouts, that.timeouts)
                && Objects.equals(this.lag, that.lag)
                && Objects.equals(this.rate, that.rate)
                && Objects.equals(this.throughput, that.throughput);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.topicQualifiedName, this.name, this.delivered, this.discarded, this.inflight,
                this.timeouts, this.lag, this.rate, this.throughput);
    }
}
