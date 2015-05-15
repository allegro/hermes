package pl.allegro.tech.hermes.consumers.supervisor;

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
}

final class SubscriptionLock {
}
