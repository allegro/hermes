package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Optional;

public interface SubscriptionAssignmentAware {
    default void onSubscriptionAssigned(SubscriptionName subscriptionName) {}
    default void onAssignmentRemoved(SubscriptionName subscriptionName) {}
    // TODO - in implementing classes create consumerId() method that is called by this one
    // so there is no confusion
    Optional<String> watchedConsumerId();
}
