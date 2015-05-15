package pl.allegro.tech.hermes.consumers.subscription.cache;

import java.util.Collection;

public interface SubscriptionsCache {

    void start(Collection<? extends SubscriptionCallback> callbacks);
    void stop();

}
