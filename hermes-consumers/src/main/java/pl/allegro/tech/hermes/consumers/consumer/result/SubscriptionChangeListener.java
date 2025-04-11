package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;

public interface SubscriptionChangeListener {
  void updateSubscription(Subscription subscription);
}
