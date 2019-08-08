package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Map;

import static java.util.Collections.emptyMap;

public class WorkloadConstraints {

    private final Map<SubscriptionName, SubscriptionConstraints> subscriptionConstraintsBySubscriptionName;
    private final int consumersPerSubscription;
    private final int maxSubscriptionsPerConsumer;

    public WorkloadConstraints(Map<SubscriptionName, SubscriptionConstraints> subscriptionConstraintsBySubscriptionName,
                               int consumersPerSubscription,
                               int maxSubscriptionsPerConsumer) {
        this.subscriptionConstraintsBySubscriptionName = subscriptionConstraintsBySubscriptionName;
        this.consumersPerSubscription = consumersPerSubscription;
        this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
    }

    public SubscriptionConstraints getSubscriptionConstraints(SubscriptionName subscriptionName) {
        return subscriptionConstraintsBySubscriptionName
                .getOrDefault(subscriptionName, new SubscriptionConstraints(subscriptionName, consumersPerSubscription));
    }

    public int getConsumersNumber(SubscriptionName subscriptionName) {
        return getSubscriptionConstraints(subscriptionName).getConsumersNumber();
    }

    public int getConsumersPerSubscription() {
        return consumersPerSubscription;
    }

    public int getMaxSubscriptionsPerConsumer() {
        return maxSubscriptionsPerConsumer;
    }

    public static WorkloadConstraints defaultConstraints(int consumersPerSubscription, int maxSubscriptionsPerConsumer) {
        return new WorkloadConstraints(emptyMap(), consumersPerSubscription, maxSubscriptionsPerConsumer);
    }
}
