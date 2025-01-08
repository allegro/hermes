package pl.allegro.tech.hermes.domain.workload.constraints;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;

public class ConsumersWorkloadConstraints {

  private final Map<TopicName, Constraints> topicConstraints;
  private final Map<SubscriptionName, Constraints> subscriptionConstraints;

  public ConsumersWorkloadConstraints(
      Map<TopicName, Constraints> topicConstraints,
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsumersWorkloadConstraints that = (ConsumersWorkloadConstraints) o;
    return Objects.equals(topicConstraints, that.topicConstraints)
        && Objects.equals(subscriptionConstraints, that.subscriptionConstraints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(topicConstraints, subscriptionConstraints);
  }
}
