package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;

public interface SubscriptionChangeAwareSuccessHandler extends SuccessHandler {
  void updateSubscription(Subscription subscription);
}
