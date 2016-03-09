package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import pl.allegro.tech.hermes.api.helpers.Patch;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Map;
import java.util.Objects;

public class SubscriptionPolicy {

    private static final int DEFAULT_RATE = 400;
    private static final int DEFAULT_MESSAGE_TTL = 3600;
    private static final int DEFAULT_MESSAGE_BACKOFF = 100;

    @Min(1)
    private int rate;

    @Min(0)
    @Max(7200)
    private int messageTtl = 3600;

    @Min(0)
    private int messageBackoff = 100;

    private boolean retryClientErrors = false;

    private SubscriptionPolicy() { }

    public SubscriptionPolicy(int rate,
                              int messageTtl,
                              boolean retryClientErrors,
                              int messageBackoff) {
        this.rate = rate;
        this.messageTtl = messageTtl;
        this.retryClientErrors = retryClientErrors;
        this.messageBackoff = messageBackoff;
    }

    @JsonCreator
    public static SubscriptionPolicy create(Map<String, Object> properties) {
        return new SubscriptionPolicy(
                (Integer) properties.getOrDefault("rate", DEFAULT_RATE),
                (Integer) properties.getOrDefault("messageTtl", DEFAULT_MESSAGE_TTL),
                (Boolean) properties.getOrDefault("retryClientErrors", false),
                (Integer) properties.getOrDefault("messageBackoff", DEFAULT_MESSAGE_BACKOFF)
        );
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
                && Objects.equals(this.messageBackoff, other.messageBackoff)
                && Objects.equals(this.retryClientErrors, other.retryClientErrors);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("rate", rate)
                .add("messageTtl", messageTtl)
                .add("messageBackoff", messageBackoff)
                .add("retryClientErrors", retryClientErrors)
                .toString();
    }

    public Integer getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public Integer getMessageTtl() {
        return messageTtl;
    }

    public Boolean isRetryClientErrors() {
        return retryClientErrors;
    }

    public Integer getMessageBackoff() {
        return messageBackoff;
    }

    public static class Builder {

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

        public Builder withMessageBackoff(int backoff) {
            subscriptionPolicy.messageBackoff = backoff;
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

        public <T> Builder applyPatch(PatchData update) {
            if (update != null) {
                subscriptionPolicy = Patch.apply(subscriptionPolicy, update);
            }
            return this;
        }
    }
}
