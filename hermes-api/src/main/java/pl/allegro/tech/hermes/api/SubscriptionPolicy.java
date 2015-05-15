package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.api.helpers.Patch;

import javax.validation.constraints.Min;
import java.util.Objects;

public class SubscriptionPolicy {

    @Min(1)
    private Integer rate;

    @Min(0)
    private Integer messageTtl;

    private boolean retryClientErrors = false;

    private SubscriptionPolicy() { }

    @JsonCreator
    public SubscriptionPolicy(@JsonProperty("rate") int rate, @JsonProperty("messageTtl") int messageTtl,  @JsonProperty("retryClientErrors") boolean retryClientErrors) {
        this.rate = rate;
        this.messageTtl = messageTtl;
        this.retryClientErrors = retryClientErrors;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rate, messageTtl);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final SubscriptionPolicy other = (SubscriptionPolicy) obj;
        return Objects.equals(this.rate, other.rate)
                && Objects.equals(this.messageTtl, other.messageTtl)
                && Objects.equals(this.retryClientErrors, other.retryClientErrors);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("rate", rate)
                .add("messageTtl", messageTtl)
                .add("retryClientErrors", retryClientErrors)
                .toString();
    }

    //<editor-fold desc="Getters/Setters">
    public Integer getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public Integer getMessageTtl() {
        return messageTtl;
    }

    public boolean isRetryClientErrors() {
        return retryClientErrors;
    }
    //</editor-fold>

    public static class Builder {
        private static final Integer  DEFAULT_RATE = 400;
        private static final Integer  DEFAULT_MESSAGE_TTL = 3600;

        private SubscriptionPolicy subscriptionPolicy;

        public Builder() {
            subscriptionPolicy = new SubscriptionPolicy();
        }

        public Builder applyDefaults() {
            subscriptionPolicy.rate = DEFAULT_RATE;
            subscriptionPolicy.messageTtl = DEFAULT_MESSAGE_TTL;
            return this;
        }

        public Builder withRate(int rate) {
            subscriptionPolicy.rate = rate;
            return this;
        }

        public Builder withMessageTtl(int ttl) {
            subscriptionPolicy.messageTtl = ttl;
            return this;
        }

        public Builder withClientErrorRetry() {
            subscriptionPolicy.retryClientErrors = true;
            return this;
        }

        public static Builder subscriptionPolicy() {
            return new Builder();
        }

        public SubscriptionPolicy build() {
            return subscriptionPolicy;
        }

        public Builder applyPatch(SubscriptionPolicy update) {
            if (update != null) {
                subscriptionPolicy = Patch.apply(subscriptionPolicy, update);
            }
            return this;
        }
    }
}
