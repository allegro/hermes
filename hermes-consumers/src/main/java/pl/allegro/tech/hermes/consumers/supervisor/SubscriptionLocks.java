package pl.allegro.tech.hermes.consumers.supervisor;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

class SubscriptionLocks {

    private Map<String, CloseableSubscriptionLock> locks = new HashMap<>();

    private synchronized CloseableSubscriptionLock getLock(String subscriptionId) {
        if (!locks.containsKey(subscriptionId)) {
            locks.put(subscriptionId, new CloseableSubscriptionLock());
        }

        return locks.get(subscriptionId);
    }

    public CloseableSubscriptionLock lock(SubscriptionName subscriptionName) {
        return getLock(Subscription.getId(subscriptionName.getTopicName(), subscriptionName.getName())).autoLock();
    }

    public CloseableSubscriptionLock lock(Subscription subscription) {
        return getLock(subscription.getId()).autoLock();
    }
}

final class CloseableSubscriptionLock extends ReentrantLock implements AutoCloseable {

    private static final boolean accessOrderEnabled = true;

    public CloseableSubscriptionLock() {
        super(accessOrderEnabled);
    }

    public CloseableSubscriptionLock autoLock() {
        this.lock();
        return this;
    }

    @Override
    public void close() {
        this.unlock();
    }
}
