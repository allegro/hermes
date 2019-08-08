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
        this.subscriptionConstraints = subscriptionConstraints;
        this.topicConstraints = topicConstraints;
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

    public static WorkloadConstraintsBuilder builder() {
        return new WorkloadConstraintsBuilder();
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

    public static class WorkloadConstraintsBuilder {
        private List<SubscriptionConstraints> subscriptionConstraints;
        private List<TopicConstraints> topicConstraints;
        private int consumersPerSubscription;
        private int maxSubscriptionsPerConsumer;
        private int availableConsumers;

        public WorkloadConstraints build() {
            return new WorkloadConstraints(subscriptionConstraints, topicConstraints, consumersPerSubscription, maxSubscriptionsPerConsumer, availableConsumers);
        }

        public WorkloadConstraintsBuilder withSubscriptionConstraints(List<SubscriptionConstraints> subscriptionConstraints) {
            this.subscriptionConstraints = subscriptionConstraints;
            return this;
        }

        public WorkloadConstraintsBuilder withTopicConstraints(List<TopicConstraints> topicConstraints) {
            this.topicConstraints = topicConstraints;
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
