package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SubscriptionMetrics {
    private long delivered;
    private long discarded;
    private long volume;
    private long inflight;
    private MetricDecimalValue timeouts;
    private MetricDecimalValue otherErrors;
    private MetricDecimalValue codes2xx;
    private MetricDecimalValue codes4xx;
    private MetricDecimalValue codes5xx;
    private MetricLongValue lag;
    private Subscription.State state;
    private MetricDecimalValue rate;
    private MetricDecimalValue throughput;
    private MetricDecimalValue batchRate;

    private SubscriptionMetrics() {
    }

    @JsonCreator
    public SubscriptionMetrics(@JsonProperty("delivered") long delivered,
                               @JsonProperty("discarded") long discarded,
                               @JsonProperty("volume") long volume,
                               @JsonProperty("inflight") long inflight,
                               @JsonProperty("timeouts") MetricDecimalValue timeouts,
                               @JsonProperty("otherErrors") MetricDecimalValue otherErrors,
                               @JsonProperty("codes2xx") MetricDecimalValue codes2xx,
                               @JsonProperty("codes4xx") MetricDecimalValue codes4xx,
                               @JsonProperty("codes5xx") MetricDecimalValue codes5xx,
                               @JsonProperty("Subscription") Subscription.State state,
                               @JsonProperty("rate") MetricDecimalValue rate,
                               @JsonProperty("throughput") MetricDecimalValue throughput,
                               @JsonProperty("batchRate") MetricDecimalValue batchRate) {
        this.delivered = delivered;
        this.discarded = discarded;
        this.volume = volume;
        this.inflight = inflight;
        this.timeouts = timeouts;
        this.otherErrors = otherErrors;
        this.codes2xx = codes2xx;
        this.codes4xx = codes4xx;
        this.codes5xx = codes5xx;
        this.state = state;
        this.rate = rate;
        this.throughput = throughput;
        this.batchRate = batchRate;
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

    public MetricDecimalValue getTimeouts() {
        return timeouts;
    }

    public MetricLongValue getLag() {
        return lag;
    }

    public MetricDecimalValue getRate() {
        return rate;
    }
    
    public MetricDecimalValue getOtherErrors() {
        return otherErrors;
    }

    public MetricDecimalValue getCodes2xx() {
        return codes2xx;
    }

    public MetricDecimalValue getCodes4xx() {
        return codes4xx;
    }

    public MetricDecimalValue getCodes5xx() {
        return codes5xx;
    }

    public Subscription.State getState() {
        return state;
    }

    public MetricDecimalValue getThroughput() {
        return throughput;
    }

    public MetricDecimalValue getBatchRate() {
        return batchRate;
    }

    public long getVolume() {
        return volume;
    }

    public static class Builder {
        private SubscriptionMetrics subscriptionMetrics;

        public Builder() {
            subscriptionMetrics = new SubscriptionMetrics();
        }

        public Builder withDelivered(long delivered) {
            subscriptionMetrics.delivered = delivered;
            return this;
        }

        public Builder withDiscarded(long discarded) {
            subscriptionMetrics.discarded = discarded;
            return this;
        }

        public Builder withVolume(long volume) {
            subscriptionMetrics.volume = volume;
            return this;
        }

        public Builder withInflight(long inflight) {
            subscriptionMetrics.inflight = inflight;
            return this;
        }

        public Builder withOtherErrors(MetricDecimalValue otherErrors) {
            subscriptionMetrics.otherErrors = otherErrors;
            return this;
        }

        public Builder withTimeouts(MetricDecimalValue timeouts) {
            subscriptionMetrics.timeouts = timeouts;
            return this;
        }

        public Builder withCodes2xx(MetricDecimalValue count) {
            subscriptionMetrics.codes2xx = count;
            return this;
        }

        public Builder withCodes4xx(MetricDecimalValue count) {
            subscriptionMetrics.codes4xx = count;
            return this;
        }

        public Builder withCodes5xx(MetricDecimalValue count) {
            subscriptionMetrics.codes5xx = count;
            return this;
        }

        public Builder withRate(MetricDecimalValue rate) {
            subscriptionMetrics.rate = rate;
            return this;
        }

        public Builder withState(Subscription.State state) {
            subscriptionMetrics.state = state;
            return this;
        }

        public Builder withLag(MetricLongValue lag) {
            subscriptionMetrics.lag = lag;
            return this;
        }

        public Builder withThroughput(MetricDecimalValue throughput) {
            subscriptionMetrics.throughput = throughput;
            return this;
        }

        public Builder withBatchRate(MetricDecimalValue batchRate) {
            subscriptionMetrics.batchRate = batchRate;
            return this;
        }

        public static Builder subscriptionMetrics() {
            return new Builder();
        }

        public SubscriptionMetrics build() {
            return subscriptionMetrics;
        }
    }
}
