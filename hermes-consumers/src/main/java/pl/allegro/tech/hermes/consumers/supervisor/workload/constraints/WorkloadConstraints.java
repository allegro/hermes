package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class WorkloadConstraints {

    private final List<SubscriptionConstraints> subscriptionConstraints;
    private final int consumersPerSubscription;
    private final int maxSubscriptionsPerConsumer;
    private final int availableConsumers;

    public WorkloadConstraints(List<SubscriptionConstraints> subscriptionConstraints,
                               int consumersPerSubscription,
                               int maxSubscriptionsPerConsumer,
                               int availableConsumers) {
        this.subscriptionConstraints = subscriptionConstraints;
        this.consumersPerSubscription = consumersPerSubscription;
        this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
        this.availableConsumers = availableConsumers;
    }

    public int getConsumersNumber(SubscriptionName subscriptionName) {
        final int requiredConsumers = getSubscriptionConstraints(subscriptionName)
                .map(SubscriptionConstraints::getConsumersNumber)
                .orElse(consumersPerSubscription);
        if (requiredConsumers > availableConsumers) {
            return consumersPerSubscription;
        }
        return requiredConsumers;
    }

    public int getMaxSubscriptionsPerConsumer() {
        return maxSubscriptionsPerConsumer;
    }

    public static WorkloadConstraints defaultConstraints(int consumersPerSubscription, int maxSubscriptionsPerConsumer, int availableConsumers) {
        return new WorkloadConstraints(emptyList(), consumersPerSubscription, maxSubscriptionsPerConsumer, availableConsumers);
    }

    public static WorkloadConstraintsBuilder builder() {
        return new WorkloadConstraintsBuilder();
    }

    private Optional<SubscriptionConstraints> getSubscriptionConstraints(SubscriptionName subscriptionName) {
        return subscriptionConstraints.stream()
                .filter(sub -> sub.getSubscriptionName().equals(subscriptionName))
                .findFirst();
    }

    public static class WorkloadConstraintsBuilder {
        private List<SubscriptionConstraints> subscriptionConstraints;
        private int consumersPerSubscription;
        private int maxSubscriptionsPerConsumer;
        private int availableConsumers;

        public WorkloadConstraints build() {
            return new WorkloadConstraints(subscriptionConstraints, consumersPerSubscription, maxSubscriptionsPerConsumer, availableConsumers);
        }

        public WorkloadConstraintsBuilder withSubscriptionConstraints(List<SubscriptionConstraints> subscriptionConstraints) {
            this.subscriptionConstraints = subscriptionConstraints;
            return this;
        }

        public WorkloadConstraintsBuilder withConsumersPerSubscription(int consumersPerSubscription) {
            this.consumersPerSubscription = consumersPerSubscription;
            return this;
        }

        public WorkloadConstraintsBuilder withMaxSubscriptionsPerConsumer(int maxSubscriptionsPerConsumer) {
            this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
            return this;
        }

        public WorkloadConstraintsBuilder withAvailableConsumers(int availableConsumers) {
            this.availableConsumers = availableConsumers;
            return this;
        }
    }
}
