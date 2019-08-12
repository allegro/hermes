package pl.allegro.tech.hermes.consumers.subscription.id;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Optional;

public interface SubscriptionIds {

    Optional<SubscriptionId> getSubscriptionId(SubscriptionName subscriptionName);

    void start();
}
