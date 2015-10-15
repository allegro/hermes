package pl.allegro.tech.hermes.consumers.supervisor.workTracking;

import com.google.common.collect.ImmutableList;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;

public class LegacyMirroringSupervisorController implements SupervisorController {
    private final ConsumersSupervisor supervisor;
    private final SubscriptionsCache subscriptionsCache;

    public LegacyMirroringSupervisorController(ConsumersSupervisor supervisor,
                                               SubscriptionsCache subscriptionsCache) {
        this.supervisor = supervisor;
        this.subscriptionsCache = subscriptionsCache;
    }

    @Override
    public void onSubscriptionCreated(Subscription subscription) {
        supervisor.assignConsumerForSubscription(subscription);
    }

    @Override
    public void onSubscriptionRemoved(Subscription subscription) {
        supervisor.deleteConsumerForSubscriptionName(subscription.toSubscriptionName());
    }

    @Override
    public void onSubscriptionChanged(Subscription subscription) {
        supervisor.notifyConsumerOnSubscriptionUpdate(subscription);
    }


    @Override
    public void start() throws Exception {
        subscriptionsCache.start(ImmutableList.of(this));
        supervisor.start();
    }

    @Override
    public void shutdown() throws InterruptedException {
        supervisor.shutdown();
    }
}
