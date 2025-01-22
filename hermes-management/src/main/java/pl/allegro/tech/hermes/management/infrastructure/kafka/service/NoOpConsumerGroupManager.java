package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupManager;

public class NoOpConsumerGroupManager implements ConsumerGroupManager {
  @Override
  public void createConsumerGroup(Topic topic, Subscription subscription) {
    // no operation
  }

  @Override
  public void deleteConsumerGroup(SubscriptionName subscriptionName) {
    // no operation
  }
}
