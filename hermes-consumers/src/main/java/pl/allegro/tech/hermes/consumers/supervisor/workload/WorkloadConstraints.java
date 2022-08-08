package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;

public class WorkloadConstraints {

    private final int activeConsumerCount;
    private final int consumersPerSubscription;
    private final int maxSubscriptionsPerConsumer;
    private final Map<SubscriptionName, Constraints> subscriptionConstraints;
    private final Map<TopicName, Constraints> topicConstraints;

    public WorkloadConstraints(int activeConsumerCount,
                               int consumersPerSubscription,
                               int maxSubscriptionsPerConsumer,
                               Map<SubscriptionName, Constraints> subscriptionConstraints,
                               Map<TopicName, Constraints> topicConstraints) {
        this.activeConsumerCount = activeConsumerCount;
        this.consumersPerSubscription = consumersPerSubscription;
        this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
        this.subscriptionConstraints = subscriptionConstraints != null ? subscriptionConstraints : emptyMap();
        this.topicConstraints = topicConstraints != null ? topicConstraints : emptyMap();
    }

    public int getConsumerCount(SubscriptionName subscriptionName) {
        Constraints requiredConsumers = subscriptionConstraints.get(subscriptionName);
        if (requiredConsumers == null) {
            requiredConsumers = topicConstraints.get(subscriptionName.getTopicName());
        }
        if (requiredConsumers != null && requiredConsumers.getConsumersNumber() > 0) {
            return Math.min(requiredConsumers.getConsumersNumber(), activeConsumerCount);
        }
        return Math.min(consumersPerSubscription, activeConsumerCount);
    }

    public int getMaxSubscriptionsPerConsumer() {
        return maxSubscriptionsPerConsumer;
    }
}
