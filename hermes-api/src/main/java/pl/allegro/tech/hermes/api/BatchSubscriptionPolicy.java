package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.MoreObjects;
import jakarta.validation.constraints.Min;
import java.util.Map;
import java.util.Objects;
import pl.allegro.tech.hermes.api.helpers.Patch;

public class BatchSubscriptionPolicy {

  private static final int DEFAULT_MESSAGE_TTL = 60;
  private static final int DEFAULT_MESSAGE_BACKOFF = 500;
  private static final int DEFAULT_REQUEST_TIMEOUT = 30 * 1000;
  private static final int DEFAULT_BATCH_SIZE = 100;
  private static final int DEFAULT_BATCH_TIME = 30 * 1000;
  private static final int DEFAULT_BATCH_VOLUME = 64 * 1000;

  @Min(0)
  private int messageTtl;

  private boolean retryClientErrors;

  @Min(0)
  private int messageBackoff;

  @Min(1)
  private int requestTimeout;

  @Min(1)
  private int batchSize;

  @Min(1)
  private int batchTime;

  @Min(1)
  private int batchVolume;

  private BatchSubscriptionPolicy() {}

  public BatchSubscriptionPolicy(
      int messageTtl,
      boolean retryClientErrors,
      int messageBackoff,
      int requestTimeout,
      int batchSize,
      int batchTime,
      int batchVolume) {
    this.messageTtl = messageTtl;
    this.retryClientErrors = retryClientErrors;
    this.messageBackoff = messageBackoff;
    this.requestTimeout = requestTimeout;
    this.batchSize = batchSize;
    this.batchTime = batchTime;
    this.batchVolume = batchVolume;
  }

  @JsonCreator
  public static BatchSubscriptionPolicy create(Map<String, Object> properties) {
    return new BatchSubscriptionPolicy(
        (Integer) properties.getOrDefault("messageTtl", DEFAULT_MESSAGE_TTL),
        (Boolean) properties.getOrDefault("retryClientErrors", false),
        (Integer) properties.getOrDefault("messageBackoff", DEFAULT_MESSAGE_BACKOFF),
        (Integer) properties.getOrDefault("requestTimeout", DEFAULT_REQUEST_TIMEOUT),
        (Integer) properties.getOrDefault("batchSize", DEFAULT_BATCH_SIZE),
        (Integer) properties.getOrDefault("batchTime", DEFAULT_BATCH_TIME),
        (Integer) properties.getOrDefault("batchVolume", DEFAULT_BATCH_VOLUME));
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        messageTtl,
        retryClientErrors,
        messageBackoff,
        requestTimeout,
        batchSize,
        batchTime,
        batchVolume);
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
    return MoreObjects.toStringHelper(this)
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
      return this;
    }

    public <T> Builder applyPatch(PatchData patch) {
      if (patch != null) {
        subscriptionPolicy = Patch.apply(subscriptionPolicy, patch);
      }
      return this;
    }
  }
}
