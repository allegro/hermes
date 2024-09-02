package pl.allegro.tech.hermes.consumers.supervisor;

import java.util.Set;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;

public interface ConsumersSupervisor {

  void assignConsumerForSubscription(Subscription subscription);

  void deleteConsumerForSubscriptionName(SubscriptionName subscription);

  void updateSubscription(Subscription subscription);

  void updateTopic(Subscription subscription, Topic topic);

  void shutdown() throws InterruptedException;

  void retransmit(SubscriptionName subscription) throws Exception;

  Set<SubscriptionName> runningConsumers();

  void start() throws Exception;
}
