package pl.allegro.tech.hermes.domain.subscription;

import java.util.Collection;
import java.util.List;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;

public interface SubscriptionRepository {

  boolean subscriptionExists(TopicName topicName, String subscriptionName);

  void ensureSubscriptionExists(TopicName topicName, String subscriptionName);

  void createSubscription(Subscription subscription);

  void removeSubscription(TopicName topicName, String subscriptionName);

  void updateSubscription(Subscription modifiedSubscription);

  void updateSubscriptionState(
      TopicName topicName, String subscriptionName, Subscription.State state);

  Subscription getSubscriptionDetails(TopicName topicName, String subscriptionName);

  Subscription getSubscriptionDetails(SubscriptionName subscriptionNames);

  List<Subscription> getSubscriptionDetails(Collection<SubscriptionName> subscriptionNames);

  List<String> listSubscriptionNames(TopicName topicName);

  List<Subscription> listSubscriptions(TopicName topicName);

  List<Subscription> listAllSubscriptions();
}
