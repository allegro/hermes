package pl.allegro.tech.hermes.benchmark.consumer;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerAuthorizationHandler;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

public class NoOpConsumerAuthorizationHandler implements ConsumerAuthorizationHandler {
  @Override
  public void createSubscriptionHandler(SubscriptionName subscriptionName) {}

  @Override
  public void removeSubscriptionHandler(SubscriptionName subscriptionName) {}

  @Override
  public void updateSubscription(SubscriptionName subscriptionName) {}

  @Override
  public void handleDiscarded(
      Message message, Subscription subscription, MessageSendingResult result) {}

  @Override
  public void handleFailed(
      Message message, Subscription subscription, MessageSendingResult result) {}

  @Override
  public void handleSuccess(
      Message message, Subscription subscription, MessageSendingResult result) {}
}
