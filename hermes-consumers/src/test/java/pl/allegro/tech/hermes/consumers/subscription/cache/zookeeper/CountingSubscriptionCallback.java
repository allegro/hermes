package pl.allegro.tech.hermes.consumers.subscription.cache.zookeeper;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionCallback;

import java.util.concurrent.CountDownLatch;

public class CountingSubscriptionCallback implements SubscriptionCallback {
    private final CountDownLatch createLatch = new CountDownLatch(1);
    private final CountDownLatch removeLatch = new CountDownLatch(1);
    private final CountDownLatch changeLatch = new CountDownLatch(1);

    @Override
    public void onSubscriptionCreated(Subscription subscription) {
        createLatch.countDown();
    }

    @Override
    public void onSubscriptionRemoved(Subscription subscription) {
        removeLatch.countDown();
    }

    @Override
    public void onSubscriptionChanged(Subscription subscription) {
        changeLatch.countDown();
    }

    public CountDownLatch getCreateLatch() {
        return createLatch;
    }

    public CountDownLatch getRemoveLatch() {
        return removeLatch;
    }

    public CountDownLatch getChangeLatch() {
        return changeLatch;
    }
}
