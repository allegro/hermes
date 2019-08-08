package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Map;

import static java.util.Collections.emptyMap;

public class WorkloadConstraints {

    private final Map<SubscriptionName, SubscriptionConstraints> subscriptionConstraintsBySubscriptionName;
    private final int consumersPerSubscription;
    private final int maxSubscriptionsPerConsumer;
    private final int availableConsumers;

    public WorkloadConstraints(Map<SubscriptionName, SubscriptionConstraints> subscriptionConstraintsBySubscriptionName,
                               int consumersPerSubscription,
                               int maxSubscriptionsPerConsumer,
                               int availableConsumers) {
        this.subscriptionConstraintsBySubscriptionName = subscriptionConstraintsBySubscriptionName;
        this.consumersPerSubscription = consumersPerSubscription;
        this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
        this.availableConsumers = availableConsumers;
    }

    public int getConsumersNumber(SubscriptionName subscriptionName) {
        final int requiredConsumers = getSubscriptionConstraints(subscriptionName).getConsumersNumber();
        if (requiredConsumers > availableConsumers) {
            return consumersPerSubscription;
        }
        return requiredConsumers;
    }

    public int getConsumersPerSubscription() {
        return consumersPerSubscription;
    }

    public int getMaxSubscriptionsPerConsumer() {
        return maxSubscriptionsPerConsumer;
    }

    public static WorkloadConstraints defaultConstraints(int consumersPerSubscription, int maxSubscriptionsPerConsumer, int availableConsumers) {
        return new WorkloadConstraints(emptyMap(), consumersPerSubscription, maxSubscriptionsPerConsumer, availableConsumers);
    }

    private SubscriptionConstraints getSubscriptionConstraints(SubscriptionName subscriptionName) {
        return subscriptionConstraintsBySubscriptionName
                .getOrDefault(subscriptionName, new SubscriptionConstraints(subscriptionName, consumersPerSubscription));
    }
}
