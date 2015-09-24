package pl.allegro.tech.hermes.api;

public class SubscriptionMetrics {
    private long delivered;
    private long discarded;
    private long inflight;
    private String timeouts;
    private String otherErrors;
    private String codes2xx;
    private String codes4xx;
    private String codes5xx;
    private Subscription.State state;
    private String rate = "0.0";

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

    public long getInflight() {
        return inflight;
    }

    public void setInflight(long inflight) {
        this.inflight = inflight;
    }

    public String getTimeouts() {
        return timeouts;
    }

    public void setTimeouts(String timeouts) {
        this.timeouts = timeouts;
    }

    public String getOtherErrors() {
        return otherErrors;
    }

    public void setOtherErrors(String otherErrors) {
        this.otherErrors = otherErrors;
    }

    public String getCodes2xx() {
        return codes2xx;
    }

    public void setCodes2xx(String codes2xx) {
        this.codes2xx = codes2xx;
    }

    public String getCodes4xx() {
        return codes4xx;
    }

    public void setCodes4xx(String codes4xx) {
        this.codes4xx = codes4xx;
    }

    public String getCodes5xx() {
        return codes5xx;
    }

    public void setCodes5xx(String codes5xx) {
        this.codes5xx = codes5xx;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public Subscription.State getState() {
        return state;
    }

    public void setState(Subscription.State state) {
        this.state = state;
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

        public static Builder subscriptionMetrics() {
            return new Builder();
        }

        public SubscriptionMetrics build() {
            return subscriptionMetrics;
        }
    }
}
