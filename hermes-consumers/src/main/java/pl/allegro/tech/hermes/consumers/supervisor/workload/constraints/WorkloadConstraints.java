package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class WorkloadConstraints {

    private final List<SubscriptionConstraints> subscriptionConstraints;
    private final List<TopicConstraints> topicConstraints;
    private final int consumersPerSubscription;
    private final int maxSubscriptionsPerConsumer;
    private final int availableConsumers;

    public WorkloadConstraints(List<SubscriptionConstraints> subscriptionConstraints,
                               List<TopicConstraints> topicConstraints,
                               int consumersPerSubscription,
                               int maxSubscriptionsPerConsumer,
                               int availableConsumers) {
        this.subscriptionConstraints = subscriptionConstraints != null ? subscriptionConstraints : emptyList();
        this.topicConstraints = topicConstraints != null ? topicConstraints : emptyList();
        this.consumersPerSubscription = consumersPerSubscription;
        this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
        this.availableConsumers = availableConsumers;
    }

    public int getConsumersNumber(SubscriptionName subscriptionName) {
        final int requiredConsumersForTopic = getTopicConstraints(subscriptionName.getTopicName())
                .map(TopicConstraints::getConsumersNumber)
                .orElse(0);
        if (requiredConsumersForTopic > 0 && requiredConsumersForTopic <= availableConsumers) {
            return requiredConsumersForTopic;
        }

        final int requiredConsumers = getSubscriptionConstraints(subscriptionName)
                .map(SubscriptionConstraints::getConsumersNumber)
                .orElse(consumersPerSubscription);
        if (requiredConsumers > 0 && requiredConsumers <= availableConsumers) {
            return requiredConsumers;
        }

        return consumersPerSubscription;
    }

    public int getMaxSubscriptionsPerConsumer() {
        return maxSubscriptionsPerConsumer;
    }

    public static WorkloadConstraints defaultConstraints(int consumersPerSubscription, int maxSubscriptionsPerConsumer, int availableConsumers) {
        return new WorkloadConstraints(emptyList(), emptyList(), consumersPerSubscription, maxSubscriptionsPerConsumer, availableConsumers);
    }

    private Optional<SubscriptionConstraints> getSubscriptionConstraints(SubscriptionName subscriptionName) {
        return subscriptionConstraints.stream()
                .filter(sub -> sub.getSubscriptionName().equals(subscriptionName))
                .findFirst();
    }

    private Optional<TopicConstraints> getTopicConstraints(TopicName topicName) {
        return topicConstraints.stream()
                .filter(topic -> topic.getTopicName().equals(topicName))
                .findFirst();
    }
}
