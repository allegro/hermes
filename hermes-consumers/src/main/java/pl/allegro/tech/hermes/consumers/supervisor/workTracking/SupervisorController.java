package pl.allegro.tech.hermes.consumers.supervisor.workTracking;

import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionCallback;

public interface SupervisorController extends SubscriptionCallback, SubscriptionAssignmentAware {
    void start() throws Exception;
    void shutdown() throws InterruptedException;
}
