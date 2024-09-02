package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import java.util.Objects;
import pl.allegro.tech.hermes.api.helpers.Patch;

public class SubscriptionPolicy {

  private static final int DEFAULT_RATE = 400;
  private static final int DEFAULT_MESSAGE_TTL = 3600;
  private static final int DEFAULT_MESSAGE_BACKOFF = 100;
  private static final int DEFAULT_REQUEST_TIMEOUT = 1000;
  private static final int DEFAULT_SOCKET_TIMEOUT = 0;
  private static final int DEFAULT_SENDING_DELAY = 0;
  private static final double DEFAULT_BACKOFF_MULTIPLIER = 1;
  private static final int DEFAULT_BACKOFF_MAX_INTERVAL = 600;

  @Min(1)
  private int rate = DEFAULT_RATE;

  @Min(0)
  @Max(7200)
  private int messageTtl = DEFAULT_MESSAGE_TTL;

  @Min(0)
  private int messageBackoff = DEFAULT_MESSAGE_BACKOFF;

  @Min(100)
  @Max(300_000)
  private int requestTimeout = DEFAULT_REQUEST_TIMEOUT;

  @Min(0)
  @Max(300_000)
  private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

  @Min(1)
  private Integer inflightSize;

  @Min(0)
  @Max(5000)
  private int sendingDelay = DEFAULT_SENDING_DELAY;

  @Min(1)
  @Max(10)
  private double backoffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;

  @Min(1)
  @Max(600)
  private int backoffMaxIntervalInSec = DEFAULT_BACKOFF_MAX_INTERVAL;

  private boolean retryClientErrors = false;

  private SubscriptionPolicy() {}

  public SubscriptionPolicy(
      int rate,
      int messageTtl,
      int requestTimeout,
      int socketTimeout,
      boolean retryClientErrors,
      int messageBackoff,
      Integer inflightSize,
      int sendingDelay,
      double backoffMultiplier,
      int backoffMaxIntervalInSec) {
    this.rate = rate;
    this.messageTtl = messageTtl;
    this.requestTimeout = requestTimeout;
    this.socketTimeout = socketTimeout;
    this.retryClientErrors = retryClientErrors;
    this.messageBackoff = messageBackoff;
    this.inflightSize = inflightSize;
    this.sendingDelay = sendingDelay;
    this.backoffMultiplier = backoffMultiplier;
    this.backoffMaxIntervalInSec = backoffMaxIntervalInSec;
  }

  @JsonCreator
  public static SubscriptionPolicy create(Map<String, Object> properties) {
    return new SubscriptionPolicy(
        (Integer) properties.getOrDefault("rate", DEFAULT_RATE),
        (Integer) properties.getOrDefault("messageTtl", DEFAULT_MESSAGE_TTL),
        (Integer) properties.getOrDefault("requestTimeout", DEFAULT_REQUEST_TIMEOUT),
        (Integer) properties.getOrDefault("socketTimeout", DEFAULT_SOCKET_TIMEOUT),
        (Boolean) properties.getOrDefault("retryClientErrors", false),
        (Integer) properties.getOrDefault("messageBackoff", DEFAULT_MESSAGE_BACKOFF),
        (Integer) properties.getOrDefault("inflightSize", null),
        (Integer) properties.getOrDefault("sendingDelay", DEFAULT_SENDING_DELAY),
        ((Number) properties.getOrDefault("backoffMultiplier", DEFAULT_BACKOFF_MULTIPLIER))
            .doubleValue(),
        (Integer) properties.getOrDefault("backoffMaxIntervalInSec", DEFAULT_BACKOFF_MAX_INTERVAL));
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        rate,
        messageTtl,
        messageBackoff,
        retryClientErrors,
        requestTimeout,
        socketTimeout,
        inflightSize,
        sendingDelay,
        backoffMultiplier,
        backoffMaxIntervalInSec);
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
        && Objects.equals(this.socketTimeout, other.socketTimeout)
        && Objects.equals(this.inflightSize, other.inflightSize)
        && Objects.equals(this.sendingDelay, other.sendingDelay)
        && Objects.equals(this.backoffMultiplier, other.backoffMultiplier)
        && Objects.equals(this.backoffMaxIntervalInSec, other.backoffMaxIntervalInSec);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("rate", rate)
        .add("messageTtl", messageTtl)
        .add("requestTimeout", requestTimeout)
        .add("socketTimeout", socketTimeout)
        .add("messageBackoff", messageBackoff)
        .add("retryClientErrors", retryClientErrors)
        .add("inflightSize", inflightSize)
        .add("sendingDelay", sendingDelay)
        .add("backoffMultiplier", backoffMultiplier)
        .add("backoffMaxIntervalInSec", backoffMaxIntervalInSec)
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

  public Integer getRequestTimeout() {
    return requestTimeout;
  }

  public Integer getSocketTimeout() {
    return socketTimeout;
  }

  @Nullable
  public Integer getInflightSize() {
    return inflightSize;
  }

  public Integer getSendingDelay() {
    return sendingDelay;
  }

  public Double getBackoffMultiplier() {
    return backoffMultiplier;
  }

  public Integer getBackoffMaxIntervalInSec() {
    return backoffMaxIntervalInSec;
  }

  public Long getBackoffMaxIntervalMillis() {
    return backoffMaxIntervalInSec * 1000L;
  }

  public static class Builder {

    private SubscriptionPolicy subscriptionPolicy;

    public Builder() {
      subscriptionPolicy = new SubscriptionPolicy();
    }

    public static Builder subscriptionPolicy() {
      return new Builder();
    }

    public Builder applyDefaults() {
      subscriptionPolicy.rate = DEFAULT_RATE;
      subscriptionPolicy.messageTtl = DEFAULT_MESSAGE_TTL;
      subscriptionPolicy.backoffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;
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

    public Builder withSocketTimeout(int timeout) {
      subscriptionPolicy.socketTimeout = timeout;
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

    public Builder withSendingDelay(int sendingDelay) {
      subscriptionPolicy.sendingDelay = sendingDelay;
      return this;
    }

    public Builder withBackoffMultiplier(double backoffMultiplier) {
      subscriptionPolicy.backoffMultiplier = backoffMultiplier;
      return this;
    }

    public Builder withBackoffMaxIntervalInSec(int backoffMaxIntervalInSec) {
      subscriptionPolicy.backoffMaxIntervalInSec = backoffMaxIntervalInSec;
      return this;
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
