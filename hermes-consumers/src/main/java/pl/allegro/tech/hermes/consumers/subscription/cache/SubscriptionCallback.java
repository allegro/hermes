package pl.allegro.tech.hermes.consumers.subscription.cache;

import pl.allegro.tech.hermes.api.Subscription;

public interface SubscriptionCallback {
    default void onSubscriptionCreated(Subscription subscription) {}
    default void onSubscriptionRemoved(Subscription subscription) {}
    default void onSubscriptionChanged(Subscription subscription) {}
}
