package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

import pl.allegro.tech.hermes.api.SubscriptionName;

public class SubscriptionConstraints {

    private final SubscriptionName subscriptionName;
    private final int consumersNumber;

    public SubscriptionConstraints(SubscriptionName subscriptionName, int consumersNumber) {
        this.subscriptionName = subscriptionName;
        this.consumersNumber = consumersNumber;
    }

    public SubscriptionName getSubscriptionName() {
        return subscriptionName;
    }

    public int getRequiredConsumersNumber() {
        return consumersNumber;
    }
}
