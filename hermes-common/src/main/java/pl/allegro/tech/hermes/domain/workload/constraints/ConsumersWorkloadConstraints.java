package pl.allegro.tech.hermes.domain.workload.constraints;

import com.google.common.collect.ImmutableMap;
import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.Map;

public class ConsumersWorkloadConstraints {

    private final Map<TopicName, Constraints> topicConstraints;
    private final Map<SubscriptionName, Constraints> subscriptionConstraints;

    public ConsumersWorkloadConstraints(Map<TopicName, Constraints> topicConstraints,
                                 Map<SubscriptionName, Constraints> subscriptionConstraints) {
        this.topicConstraints = ImmutableMap.copyOf(topicConstraints);
        this.subscriptionConstraints = ImmutableMap.copyOf(subscriptionConstraints);
    }

    public Map<TopicName, Constraints> getTopicConstraints() {
        return topicConstraints;
    }

    public Map<SubscriptionName, Constraints> getSubscriptionConstraints() {
        return subscriptionConstraints;
    }
}
