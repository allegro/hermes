package pl.allegro.tech.hermes.domain.workload.constraints;

import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.Map;

import static java.util.Collections.emptyMap;

public class WorkloadConstraints {

    private final Map<SubscriptionName, Constraints> subscriptionConstraints;
    private final Map<TopicName, Constraints> topicConstraints;
    private final int consumersPerSubscription;
    private final int maxSubscriptionsPerConsumer;
    private final int availableConsumers;

    public WorkloadConstraints(Map<SubscriptionName, Constraints> subscriptionConstraints,
                               Map<TopicName, Constraints> topicConstraints,
                               int consumersPerSubscription,
                               int maxSubscriptionsPerConsumer,
                               int availableConsumers) {
        this.subscriptionConstraints = subscriptionConstraints != null ? subscriptionConstraints : emptyMap();
        this.topicConstraints = topicConstraints != null ? topicConstraints : emptyMap();
        this.consumersPerSubscription = consumersPerSubscription;
        this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
        this.availableConsumers = availableConsumers;
    }

    public int getConsumersNumber(SubscriptionName subscriptionName) {
        final int requiredConsumers = subscriptionConstraints
                .getOrDefault(subscriptionName, topicConstraints.getOrDefault(subscriptionName.getTopicName(), new Constraints(consumersPerSubscription)))
                .getConsumersNumber();

        if (requiredConsumers > 0) {
            if (requiredConsumers <= availableConsumers) {
                return requiredConsumers;
            } else {
                return availableConsumers;
            }
        }

        return consumersPerSubscription;
    }

    public int getMaxSubscriptionsPerConsumer() {
        return maxSubscriptionsPerConsumer;
    }

    public static WorkloadConstraints defaultConstraints(int consumersPerSubscription, int maxSubscriptionsPerConsumer, int availableConsumers) {
        return new WorkloadConstraints(emptyMap(), emptyMap(), consumersPerSubscription, maxSubscriptionsPerConsumer, availableConsumers);
    }
}
