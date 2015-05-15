package pl.allegro.tech.hermes.consumers.subscription.cache;

import pl.allegro.tech.hermes.api.Subscription;

public interface SubscriptionCallback {
    void onSubscriptionCreated(Subscription subscription);
    void onSubscriptionRemoved(Subscription subscription);
    void onSubscriptionChanged(Subscription subscription);
}
