package pl.allegro.tech.hermes.consumers.supervisor;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.HashMap;
import java.util.Map;

class SubscriptionLocks {

    private Map<String, SubscriptionLock> locks = new HashMap<>();

    public synchronized SubscriptionLock getLock(String subscriptionId) {
        if (!locks.containsKey(subscriptionId)) {
            locks.put(subscriptionId, new SubscriptionLock());
        }

        return locks.get(subscriptionId);
    }

    public SubscriptionLock getLock(SubscriptionName subscriptionName) {
        return getLock(Subscription.getId(subscriptionName.getTopicName(), subscriptionName.getName()));
    }

    public SubscriptionLock getLock(Subscription subscription) {
        return getLock(subscription.getId());
    }
}

final class SubscriptionLock {
}
