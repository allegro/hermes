package pl.allegro.tech.hermes.api;

import java.util.Objects;

public class TopicMetrics {
    public static final String UNAVAILABLE_RATE = "unavailable";

    private long published;
    private String rate = "0.0";
    private String deliveryRate = "0.0";
    private int subscriptions;

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

    @Override
    public int hashCode() {
        return Objects.hash(published, rate, deliveryRate, subscriptions);
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
            && Objects.equals(this.subscriptions, other.subscriptions);
    }

    public static TopicMetrics unavailable() {
        return Builder.topicMetrics().withRate(UNAVAILABLE_RATE)
                                     .withDeliveryRate(UNAVAILABLE_RATE)
                                     .withPublished(0)
                                     .withSubscriptions(0)
                                     .build();
    }

    public static class Builder {
        private TopicMetrics topicMetrics;

        public Builder() {
            topicMetrics = new TopicMetrics();
        }

        public Builder withPublished(long published) {
            topicMetrics.published = published;
            return this;
        }

        public Builder withRate(String rate) {
            topicMetrics.rate = rate;
            return this;
        }

        public Builder withDeliveryRate(String deliveryRate) {
            topicMetrics.deliveryRate = deliveryRate;
            return this;
        }

        public Builder withSubscriptions(int subscriptions) {
            topicMetrics.subscriptions = subscriptions;
            return this;
        }

        public static Builder topicMetrics() {
            return new Builder();
        }

        public TopicMetrics build() {
            return topicMetrics;
        }
    }
}
