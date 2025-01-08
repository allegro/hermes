package pl.allegro.tech.hermes.consumers.supervisor.workload;

import static java.util.Collections.emptyMap;

import java.util.HashMap;
import java.util.Map;
import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;

public class WorkloadConstraints {

  private final int activeConsumerCount;
  private final int consumersPerSubscription;
  private final int maxSubscriptionsPerConsumer;
  private final Map<SubscriptionName, Constraints> subscriptionConstraints;
  private final Map<TopicName, Constraints> topicConstraints;

  private WorkloadConstraints(
      int activeConsumerCount,
      int consumersPerSubscription,
      int maxSubscriptionsPerConsumer,
      Map<SubscriptionName, Constraints> subscriptionConstraints,
      Map<TopicName, Constraints> topicConstraints) {
    this.activeConsumerCount = activeConsumerCount;
    this.consumersPerSubscription = consumersPerSubscription;
    this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
    this.subscriptionConstraints =
        subscriptionConstraints != null ? subscriptionConstraints : emptyMap();
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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private int activeConsumerCount;
    private int consumersPerSubscription;
    private int maxSubscriptionsPerConsumer;
    private Map<SubscriptionName, Constraints> subscriptionConstraints = new HashMap<>();
    private Map<TopicName, Constraints> topicConstraints = new HashMap<>();

    public Builder withActiveConsumers(int activeConsumerCount) {
      this.activeConsumerCount = activeConsumerCount;
      return this;
    }

    public Builder withConsumersPerSubscription(int consumersPerSubscription) {
      this.consumersPerSubscription = consumersPerSubscription;
      return this;
    }

    public Builder withMaxSubscriptionsPerConsumer(int maxSubscriptionsPerConsumer) {
      this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
      return this;
    }

    public Builder withSubscriptionConstraints(
        Map<SubscriptionName, Constraints> subscriptionConstraints) {
      this.subscriptionConstraints = subscriptionConstraints;
      return this;
    }

    public Builder withTopicConstraints(Map<TopicName, Constraints> topicConstraints) {
      this.topicConstraints = topicConstraints;
      return this;
    }

    public WorkloadConstraints build() {
      return new WorkloadConstraints(
          activeConsumerCount,
          consumersPerSubscription,
          maxSubscriptionsPerConsumer,
          subscriptionConstraints,
          topicConstraints);
    }
  }
}
