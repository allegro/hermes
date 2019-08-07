package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.List;

public class WorkloadConstraints {

    private final List<SubscriptionConstraints> subscriptionConstraints;

    public WorkloadConstraints(List<SubscriptionConstraints> subscriptionConstraints) {
        this.subscriptionConstraints = subscriptionConstraints;
    }

    public SubscriptionConstraints getSubscriptionConstraints(SubscriptionName subscriptionName) {
        return subscriptionConstraints.stream()
                .filter(subCons -> subCons.getSubscriptionName().equals(subscriptionName))
                .findFirst()
                .orElse(null);
    }
}
