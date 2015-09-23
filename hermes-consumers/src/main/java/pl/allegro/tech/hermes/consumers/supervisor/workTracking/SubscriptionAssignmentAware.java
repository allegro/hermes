package pl.allegro.tech.hermes.consumers.supervisor.workTracking;

import pl.allegro.tech.hermes.api.Subscription;

public interface SubscriptionAssignmentAware {
    default void onSubscriptionAssigned(Subscription subscription) {}
    default void onAssignmentRemoved(Subscription subscription) {}
}
