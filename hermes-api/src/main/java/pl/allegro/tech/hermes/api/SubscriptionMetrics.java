package pl.allegro.tech.hermes.api;

import java.util.List;
import java.util.Objects;

public class SubscriptionMetrics {
    private long delivered;
    private long discarded;
    private long inflight;
    private String timeoutsM1;
    private String otherErrorsM1;
    private HttpStatusCodeMetrics httpStatusCodesM1;
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

    public String getTimeoutsM1() {
        return timeoutsM1;
    }

    public void setTimeoutsM1(String timeoutsM1) {
        this.timeoutsM1 = timeoutsM1;
    }

    public String getOtherErrorsM1() {
        return otherErrorsM1;
    }

    public void setOtherErrorsM1(String otherErrorsM1) {
        this.otherErrorsM1 = otherErrorsM1;
    }

    public HttpStatusCodeMetrics getHttpStatusCodesM1() {
        return httpStatusCodesM1;
    }

    public void setHttpStatusCodesM1(HttpStatusCodeMetrics httpStatusCodesM1) {
        this.httpStatusCodesM1 = httpStatusCodesM1;
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
        return Objects.hash(delivered, discarded, rate, state, httpStatusCodesM1, timeoutsM1, otherErrorsM1);
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
            && Objects.equals(this.httpStatusCodesM1, other.httpStatusCodesM1)
            && Objects.equals(this.otherErrorsM1, other.otherErrorsM1)
            && Objects.equals(this.timeoutsM1, other.timeoutsM1)
            && Objects.equals(this.state, other.state);
    }

    private static class HttpStatusCodeMetrics {
        public String class1xx;
        public String class2xx;
        public String class3xx;
        public String class4xx;
        public String class5xx;

        public HttpStatusCodeMetrics() {
        }

        public HttpStatusCodeMetrics(String class1xx, String class2xx, String class3xx, String class4xx, String class5xx) {
            this.class1xx = class1xx;
            this.class2xx = class2xx;
            this.class3xx = class3xx;
            this.class4xx = class4xx;
            this.class5xx = class5xx;
        }

        public String getClass1xx() {
            return class1xx;
        }

        public void setClass1xx(String class1xx) {
            this.class1xx = class1xx;
        }

        public String getClass2xx() {
            return class2xx;
        }

        public void setClass2xx(String class2xx) {
            this.class2xx = class2xx;
        }

        public String getClass3xx() {
            return class3xx;
        }

        public void setClass3xx(String class3xx) {
            this.class3xx = class3xx;
        }

        public String getClass4xx() {
            return class4xx;
        }

        public void setClass4xx(String class4xx) {
            this.class4xx = class4xx;
        }

        public String getClass5xx() {
            return class5xx;
        }

        public void setClass5xx(String class5xx) {
            this.class5xx = class5xx;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HttpStatusCodeMetrics that = (HttpStatusCodeMetrics) o;
            return Objects.equals(class1xx, that.class1xx) &&
                    Objects.equals(class2xx, that.class2xx) &&
                    Objects.equals(class3xx, that.class3xx) &&
                    Objects.equals(class4xx, that.class4xx) &&
                    Objects.equals(class5xx, that.class5xx);
        }

        @Override
        public int hashCode() {
            return Objects.hash(class1xx, class2xx, class3xx, class4xx, class5xx);
        }
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

        public Builder withOtherErrorsM1(String otherErrors) {
            subscriptionMetrics.otherErrorsM1 = otherErrors;
            return this;
        }

        public Builder withTimeoutsM1(String timeouts) {
            subscriptionMetrics.timeoutsM1 = timeouts;
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

        public Builder withHttpStatusCodesM1(List<String> httpStatusCodes) {
            subscriptionMetrics.httpStatusCodesM1 = convertToStatusCodes(httpStatusCodes);
            return this;
        }

        private HttpStatusCodeMetrics convertToStatusCodes(List<String> httpStatusCodes) {
            return new HttpStatusCodeMetrics(httpStatusCodes.get(0), httpStatusCodes.get(1), httpStatusCodes.get(2),
                    httpStatusCodes.get(3), httpStatusCodes.get(4));
        }

        public static Builder subscriptionMetrics() {
            return new Builder();
        }

        public SubscriptionMetrics build() {
            return subscriptionMetrics;
        }
    }
}
