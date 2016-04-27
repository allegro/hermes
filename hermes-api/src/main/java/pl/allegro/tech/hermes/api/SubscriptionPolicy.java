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
    private static final int DEFAULT_REQUEST_TIMEOUT = 1000;
    private static final int DEFAULT_INFLIGHT_SIZE = 100;

    @Min(1)
    private int rate = DEFAULT_RATE;

    @Min(0)
    @Max(7200)
    private int messageTtl = DEFAULT_MESSAGE_TTL;

    @Min(0)
    private int messageBackoff = DEFAULT_MESSAGE_BACKOFF;

    @Min(100)
    @Max(60000)
    private int requestTimeout = DEFAULT_REQUEST_TIMEOUT;

    @Min(1)
    private int inflightSize = DEFAULT_INFLIGHT_SIZE;

    private boolean retryClientErrors = false;

    private SubscriptionPolicy() {
    }

    public SubscriptionPolicy(int rate,
                              int messageTtl,
                              int requestTimeout,
                              boolean retryClientErrors,
                              int messageBackoff,
                              Integer inflightSize) {
        this.rate = rate;
        this.messageTtl = messageTtl;
        this.requestTimeout = requestTimeout;
        this.retryClientErrors = retryClientErrors;
        this.messageBackoff = messageBackoff;
        this.inflightSize = inflightSize;
    }

    @JsonCreator
    public static SubscriptionPolicy create(Map<String, Object> properties) {
        return new SubscriptionPolicy(
                (Integer) properties.getOrDefault("rate", DEFAULT_RATE),
                (Integer) properties.getOrDefault("messageTtl", DEFAULT_MESSAGE_TTL),
                (Integer) properties.getOrDefault("requestTimeout", DEFAULT_REQUEST_TIMEOUT),
                (Boolean) properties.getOrDefault("retryClientErrors", false),
                (Integer) properties.getOrDefault("messageBackoff", DEFAULT_MESSAGE_BACKOFF),
                (Integer) properties.getOrDefault("inflightSize", DEFAULT_INFLIGHT_SIZE)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(rate, messageTtl, messageBackoff, retryClientErrors, requestTimeout, inflightSize);
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
                && Objects.equals(this.retryClientErrors, other.retryClientErrors)
                && Objects.equals(this.requestTimeout, other.requestTimeout)
                && Objects.equals(this.inflightSize, other.inflightSize);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("rate", rate)
                .add("messageTtl", messageTtl)
                .add("requestTimeout", requestTimeout)
                .add("messageBackoff", messageBackoff)
                .add("retryClientErrors", retryClientErrors)
                .add("inflightSize", inflightSize)
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

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public Integer getInflightSize() {
        return inflightSize;
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

        public Builder withRequestTimeout(int timeout) {
            subscriptionPolicy.requestTimeout = timeout;
            return this;
        }

        public Builder withInflightSize(Integer inflightSize) {
            subscriptionPolicy.inflightSize = inflightSize;
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
