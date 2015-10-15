package pl.allegro.tech.hermes.consumers.supervisor.workTracking;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;

public interface SubscriptionAssignmentAware {
    default void onSubscriptionAssigned(Subscription subscription) {}
    default void onAssignmentRemoved(SubscriptionName subscriptionName) {}
}
