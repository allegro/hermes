package pl.allegro.tech.hermes.consumers.supervisor.workTracking;

import pl.allegro.tech.hermes.api.SubscriptionName;

public class SubscriptionAssignment {
    private final String supervisorId;
    private final SubscriptionName subscriptionName;

    public SubscriptionAssignment(String supervisorId, SubscriptionName subscriptionName) {
        this.supervisorId = supervisorId;
        this.subscriptionName = subscriptionName;
    }

    public String getSupervisorId() {
        return supervisorId;
    }

    public SubscriptionName getSubscriptionName() {
        return subscriptionName;
    }
}