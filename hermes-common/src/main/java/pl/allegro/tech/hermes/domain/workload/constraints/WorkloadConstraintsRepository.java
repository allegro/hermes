package pl.allegro.tech.hermes.domain.workload.constraints;

import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;

public interface WorkloadConstraintsRepository {

  ConsumersWorkloadConstraints getConsumersWorkloadConstraints();

  void createConstraints(TopicName topicName, Constraints constraints);

  void createConstraints(SubscriptionName subscriptionName, Constraints constraints);

  void updateConstraints(TopicName topicName, Constraints constraints);

  void updateConstraints(SubscriptionName subscriptionName, Constraints constraints);

  void deleteConstraints(TopicName topicName);

  void deleteConstraints(SubscriptionName subscriptionName);

  boolean constraintsExist(TopicName topicName);

  boolean constraintsExist(SubscriptionName subscriptionName);
}
