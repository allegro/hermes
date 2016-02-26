package pl.allegro.tech.hermes.domain.subscription;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.query.Query;

import java.util.List;

public interface SubscriptionRepository {

    boolean subscriptionExists(TopicName topicName, String subscriptionName);

    void ensureSubscriptionExists(TopicName topicName, String subscriptionName);

    void createSubscription(Subscription subscription);

    void removeSubscription(TopicName topicName, String subscriptionName);

    void updateSubscription(Subscription modifiedSubscription);

    void updateSubscriptionState(TopicName topicName, String subscriptionName, Subscription.State state);

    Subscription getSubscriptionDetails(TopicName topicName, String subscriptionName);

    Subscription getSubscriptionDetails(SubscriptionName subscriptionId);

    List<String> listSubscriptionNames(TopicName topicName);

    List<Subscription> listSubscriptions(TopicName topicName);
}
