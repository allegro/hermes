package pl.allegro.tech.hermes.consumers.subscription.id;

import java.util.Objects;
import pl.allegro.tech.hermes.api.SubscriptionName;

public class SubscriptionId {

  private final long value;

  private final SubscriptionName subscriptionName;

  private SubscriptionId(SubscriptionName subscriptionName, long value) {
    this.value = value;
    this.subscriptionName = subscriptionName;
  }

  public static SubscriptionId from(SubscriptionName subscriptionName, long value) {
    return new SubscriptionId(subscriptionName, value);
  }

  public long getValue() {
    return value;
  }

  public SubscriptionName getSubscriptionName() {
    return subscriptionName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubscriptionId that = (SubscriptionId) o;
    return value == that.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
