package pl.allegro.tech.hermes.consumers.subscription.cache;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Collection;
import java.util.List;

public interface SubscriptionsCache {

    void start(Collection<? extends SubscriptionCallback> callbacks);
    void stop();

    List<SubscriptionName> listActiveSubscriptionNames();
}
