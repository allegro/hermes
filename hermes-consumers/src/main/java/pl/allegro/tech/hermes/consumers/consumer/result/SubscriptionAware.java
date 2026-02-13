package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;

public interface SubscriptionAware {
  default boolean appliesTo(Subscription subscription) {
    return true;
  }
}
