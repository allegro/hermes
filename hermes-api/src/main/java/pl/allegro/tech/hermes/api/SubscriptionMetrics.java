package pl.allegro.tech.hermes.api;

import java.util.Objects;

public class SubscriptionMetrics {
    private long delivered;
    private long discarded;
    private long inflight;
    private long lag;
    private String rate = "0.0";
    private Subscription.State state;

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

    public long getLag() {
        return lag;
    }

    public void setLag(long lag) {
        this.lag = lag;
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

    @Override
    public int hashCode() {
        return Objects.hash(delivered, discarded, rate, state);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final SubscriptionMetrics other = (SubscriptionMetrics) obj;

        return Objects.equals(this.delivered, other.delivered)
            && Objects.equals(this.discarded, other.discarded)
            && Objects.equals(this.rate, other.rate)
            && Objects.equals(this.state, other.state);
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

        public static Builder subscriptionMetrics() {
            return new Builder();
        }

        public SubscriptionMetrics build() {
            return subscriptionMetrics;
        }
    }
}
