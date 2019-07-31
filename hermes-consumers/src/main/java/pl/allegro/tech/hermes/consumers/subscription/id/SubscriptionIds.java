package pl.allegro.tech.hermes.consumers.subscription.id;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Optional;

public interface SubscriptionIds {

    SubscriptionId getSubscriptionId(SubscriptionName subscriptionName);

    Optional<SubscriptionName> getSubscriptionName(SubscriptionId subscriptionId);

    void start();
}
