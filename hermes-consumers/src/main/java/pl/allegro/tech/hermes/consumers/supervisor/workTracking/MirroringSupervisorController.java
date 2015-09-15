package pl.allegro.tech.hermes.consumers.supervisor.workTracking;

import com.google.common.collect.ImmutableList;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;

public class MirroringSupervisorController implements SupervisorController {
    private ConsumersSupervisor supervisor;
    private SubscriptionsCache subscriptionsCache;
    private WorkTracker workTracker;

    public MirroringSupervisorController(ConsumersSupervisor supervisor,
                                         SubscriptionsCache subscriptionsCache,
                                         WorkTracker workTracker) {
        this.supervisor = supervisor;
        this.subscriptionsCache = subscriptionsCache;
        this.workTracker = workTracker;
    }

    @Override
    public void onSubscriptionCreated(Subscription subscription) {
        workTracker.forceAssignment(subscription);
    }

    @Override
    public void onSubscriptionRemoved(Subscription subscription) {
        workTracker.dropAssignment(subscription);
    }

    @Override
    public void onSubscriptionChanged(Subscription subscription) {
        switch (subscription.getState()) {
            case PENDING:
            case ACTIVE:
                workTracker.forceAssignment(subscription);
                break;
            case SUSPENDED:
                workTracker.dropAssignment(subscription);
                break;
            default:
                break;
        }
        supervisor.updateSubscription(subscription);
    }

    @Override
    public void onSubscriptionAssigned(Subscription subscription) {
        supervisor.assignConsumerForSubscription(subscription);
    }

    @Override
    public void onAssignmentRemoved(Subscription subscription) {
        supervisor.deleteConsumerForSubscription(subscription);
    }

    @Override
    public void start() throws Exception {
        subscriptionsCache.start(ImmutableList.of(this));
        workTracker.start(ImmutableList.of(this));
        supervisor.start();
    }

    @Override
    public void shutdown() throws InterruptedException {
        supervisor.shutdown();
    }
}
