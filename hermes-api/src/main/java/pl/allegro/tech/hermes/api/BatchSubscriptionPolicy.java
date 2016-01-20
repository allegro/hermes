package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.api.helpers.Patch;

import javax.validation.constraints.Min;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class BatchSubscriptionPolicy {

    @Min(0)
    private Integer messageTtl;

    @Min(0)
    private Integer messageBackoff;

    private boolean retryClientErrors = false;

    @Min(1)
    private Integer batchSize;

    @Min(1)
    private Integer batchTime;

    @Min(1)
    private Integer batchVolume;

    @Min(1)
    private Integer requestTimeout;

    private BatchSubscriptionPolicy() {
    }

    @JsonCreator
    public BatchSubscriptionPolicy(@JsonProperty("messageTtl") Integer messageTtl,
                                   @JsonProperty("retryClientErrors") Boolean retryClientErrors,
                                   @JsonProperty("messageBackoff") Integer messageBackoff,
                                   @JsonProperty("requestTimeout") Integer requestTimeout,
                                   @JsonProperty("batchSize") Integer batchSize,
                                   @JsonProperty("batchTime") Integer batchTime,
                                   @JsonProperty("batchVolume") Integer batchVolume) {
        this.messageTtl = messageTtl;
        this.retryClientErrors = retryClientErrors;
        this.messageBackoff = messageBackoff;
        this.requestTimeout = requestTimeout;
        this.batchSize = batchSize;
        this.batchTime = batchTime;
        this.batchVolume = batchVolume;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageTtl, retryClientErrors, messageBackoff, requestTimeout, batchSize, batchTime, batchVolume);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final BatchSubscriptionPolicy other = (BatchSubscriptionPolicy) obj;
        return Objects.equals(this.messageTtl, other.messageTtl)
                && Objects.equals(this.retryClientErrors, other.retryClientErrors)
                && Objects.equals(this.messageBackoff, other.messageBackoff)
                && Objects.equals(this.requestTimeout, other.requestTimeout)
                && Objects.equals(this.batchSize, other.batchSize)
                && Objects.equals(this.batchTime, other.batchTime)
                && Objects.equals(this.batchVolume, other.batchVolume);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("messageTtl", messageTtl)
                .add("messageBackoff", messageBackoff)
                .add("retryClientErrors", retryClientErrors)
                .add("batchSize", batchSize)
                .add("batchTime", batchTime)
                .add("batchVolume", batchVolume)
                .add("requestTimeout", requestTimeout)
                .toString();
    }

    public Integer getMessageTtl() {
        return messageTtl;
    }

    public Integer getMessageBackoff() {
        return messageBackoff;
    }

    public Boolean isRetryClientErrors() {
        return retryClientErrors;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public Integer getBatchTime() {
        return batchTime;
    }

    public Integer getBatchVolume() {
        return batchVolume;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public static class Builder {

        private BatchSubscriptionPolicy subscriptionPolicy;

        public static Builder batchSubscriptionPolicy() {
            return new Builder();
        }

        public Builder() {
            subscriptionPolicy = new BatchSubscriptionPolicy();
        }

        public Builder withMessageTtl(int messageTtl) {
            subscriptionPolicy.messageTtl = messageTtl;
            return this;
        }

        public Builder withMessageBackoff(int messageBackoff) {
            subscriptionPolicy.messageBackoff = messageBackoff;
            return this;
        }

        public Builder withClientErrorRetry(boolean retryClientErrors) {
            subscriptionPolicy.retryClientErrors = retryClientErrors;
            return this;
        }

        public Builder withBatchSize(int batchSize) {
            subscriptionPolicy.batchSize = batchSize;
            return this;
        }

        public Builder withBatchTime(int batchTime) {
            subscriptionPolicy.batchTime = batchTime;
            return this;
        }

        public Builder withBatchVolume(int batchVolume) {
            subscriptionPolicy.batchVolume = batchVolume;
            return this;
        }

        public Builder withRequestTimeout(int requestTimeout) {
            subscriptionPolicy.requestTimeout = requestTimeout;
            return this;
        }

        public BatchSubscriptionPolicy build() {
            return new BatchSubscriptionPolicy(
                    subscriptionPolicy.messageTtl,
                    subscriptionPolicy.retryClientErrors,
                    subscriptionPolicy.messageBackoff,
                    subscriptionPolicy.requestTimeout,
                    subscriptionPolicy.batchSize,
                    subscriptionPolicy.batchTime,
                    subscriptionPolicy.batchVolume);
        }

        public Builder applyDefaults() {
            subscriptionPolicy.messageTtl = 60 * 1000;
            subscriptionPolicy.messageBackoff = 500;
            subscriptionPolicy.requestTimeout = 30 * 1000;
            subscriptionPolicy.batchSize = 100;
            subscriptionPolicy.batchTime = 30 * 1000;
            subscriptionPolicy.batchVolume = 64 * 1000;

            return this;
        }

        public <T> Builder applyPatch(T changes) {
            if (changes != null) {
                subscriptionPolicy = Patch.apply(subscriptionPolicy, changes);
            }
            return this;
        }
    }
}
