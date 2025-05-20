package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;

public interface SubscriptionChangeAwareSuccessHandler {
  void updateSubscription(Subscription subscription);
}
