package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.Objects;
import pl.allegro.tech.hermes.api.SubscriptionName;

final class ConsumerInstance {

  private final String consumerId;
  private final SubscriptionName subscription;

  ConsumerInstance(String consumerId, SubscriptionName subscription) {
    this.consumerId = consumerId;
    this.subscription = subscription;
  }

  public String getConsumerId() {
    return consumerId;
  }

  public SubscriptionName getSubscription() {
    return subscription;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsumerInstance that = (ConsumerInstance) o;
    return Objects.equals(consumerId, that.consumerId)
        && Objects.equals(subscription, that.subscription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(consumerId, subscription);
  }

  @Override
  public String toString() {
    return "ConsumerInstance{"
        + "consumerId='"
        + consumerId
        + '\''
        + ", subscription="
        + subscription
        + '}';
  }
}
