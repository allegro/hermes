package pl.allegro.tech.hermes.consumers.consumer;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.result.ErrorHandler;
import pl.allegro.tech.hermes.consumers.consumer.result.SuccessHandler;

public interface ConsumerAuthorizationHandler extends SuccessHandler, ErrorHandler {

  void createSubscriptionHandler(SubscriptionName subscriptionName);

  void removeSubscriptionHandler(SubscriptionName subscriptionName);

  void updateSubscription(SubscriptionName subscriptionName);
}
