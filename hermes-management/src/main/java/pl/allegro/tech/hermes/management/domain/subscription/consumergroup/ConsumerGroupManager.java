package pl.allegro.tech.hermes.management.domain.subscription.consumergroup;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;

public interface ConsumerGroupManager {

  void createConsumerGroup(Topic topic, Subscription subscription);

  void deleteConsumerGroup(SubscriptionName subscriptionName);
}
