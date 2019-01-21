package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SubscriptionMetrics {
    private long delivered;
    private long discarded;
    private long inflight;
    private String timeouts;
    private String otherErrors;
    private String codes2xx;
    private String codes4xx;
    private String codes5xx;
    private long lag;
    private Subscription.State state;
    private String rate;
    private String throughput;
    private String batchRate;

    private SubscriptionMetrics() {
    }

    @JsonCreator
    public SubscriptionMetrics(@JsonProperty("delivered") long delivered,
                               @JsonProperty("discarded") long discarded,
                               @JsonProperty("inflight") long inflight,
                               @JsonProperty("timeouts") String timeouts,
                               @JsonProperty("otherErrors") String otherErrors,
                               @JsonProperty("codes2xx") String codes2xx,
                               @JsonProperty("codes4xx") String codes4xx,
                               @JsonProperty("codes5xx") String codes5xx,
                               @JsonProperty("Subscription") Subscription.State state,
                               @JsonProperty("rate") String rate,
                               @JsonProperty("throughput") String throughput,
                               @JsonProperty("batchRate") String batchRate) {
        this.delivered = delivered;
        this.discarded = discarded;
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

    public String getTimeouts() {
        return timeouts;
    }

    public long getLag() {
        return lag;
    }

    public String getRate() {
        return rate;
    }
    
    public String getOtherErrors() {
        return otherErrors;
    }

    public String getCodes2xx() {
        return codes2xx;
    }

    public String getCodes4xx() {
        return codes4xx;
    }

    public String getCodes5xx() {
        return codes5xx;
    }

    public Subscription.State getState() {
        return state;
    }

    public String getThroughput() {
        return throughput;
    }

    public String getBatchRate() {
        return batchRate;
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

        public Builder withInflight(long inflight) {
            subscriptionMetrics.inflight = inflight;
            return this;
        }

        public Builder withOtherErrors(String otherErrors) {
            subscriptionMetrics.otherErrors = otherErrors;
            return this;
        }

        public Builder withTimeouts(String timeouts) {
            subscriptionMetrics.timeouts = timeouts;
            return this;
        }

        public Builder withCodes2xx(String count) {
            subscriptionMetrics.codes2xx = count;
            return this;
        }

        public Builder withCodes4xx(String count) {
            subscriptionMetrics.codes4xx = count;
            return this;
        }

        public Builder withCodes5xx(String count) {
            subscriptionMetrics.codes5xx = count;
            return this;
        }

        public Builder withRate(String rate) {
            subscriptionMetrics.rate = rate;
            return this;
        }

        public Builder withState(Subscription.State state) {
            subscriptionMetrics.state = state;
            return this;
        }

        public Builder withLag(long lag) {
            subscriptionMetrics.lag = lag;
            return this;
        }

        public Builder withThroughput(String throughput) {
            subscriptionMetrics.throughput = throughput;
            return this;
        }

        public Builder withBatchRate(String batchRate) {
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
