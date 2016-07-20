package pl.allegro.tech.hermes.consumers.subscription.cache;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.Collection;
import java.util.List;

public interface SubscriptionsCache {

    Subscription getSubscription(SubscriptionName subscriptionName);

    List<Subscription> subscriptionsOfTopic(TopicName topicName);

    List<SubscriptionName> listActiveSubscriptionNames();

    void start();
}
