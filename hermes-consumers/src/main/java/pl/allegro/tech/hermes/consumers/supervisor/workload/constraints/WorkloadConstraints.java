package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.Map;
import java.util.Optional;

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
        final int requiredConsumers = getSubscriptionConstraints(subscriptionName)
                .map(Constraints::getConsumersNumber)
                .orElse(0);
        if (requiredConsumers > 0) {
            if (requiredConsumers <= availableConsumers) {
                return requiredConsumers;
            } else {
                return availableConsumers;
            }
        }

        final int requiredConsumersForTopic = getTopicConstraints(subscriptionName.getTopicName())
                .map(Constraints::getConsumersNumber)
                .orElse(0);
        if (requiredConsumersForTopic > 0) {
            if (requiredConsumersForTopic <= availableConsumers) {
                return requiredConsumersForTopic;
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

    private Optional<Constraints> getSubscriptionConstraints(SubscriptionName subscriptionName) {
        return Optional.ofNullable(subscriptionConstraints.get(subscriptionName));
    }

    private Optional<Constraints> getTopicConstraints(TopicName topicName) {
        return Optional.ofNullable(topicConstraints.get(topicName));
    }
}
