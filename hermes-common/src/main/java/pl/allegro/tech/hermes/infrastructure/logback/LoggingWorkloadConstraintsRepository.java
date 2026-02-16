package pl.allegro.tech.hermes.infrastructure.logback;

import static pl.allegro.tech.hermes.common.logging.LoggingFields.SUBSCRIPTION_NAME;
import static pl.allegro.tech.hermes.common.logging.LoggingFields.TOPIC_NAME;

import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.logging.LoggingContext;
import pl.allegro.tech.hermes.domain.workload.constraints.ConsumersWorkloadConstraints;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;

public class LoggingWorkloadConstraintsRepository implements WorkloadConstraintsRepository {

  private final WorkloadConstraintsRepository delegate;

  public LoggingWorkloadConstraintsRepository(WorkloadConstraintsRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  public ConsumersWorkloadConstraints getConsumersWorkloadConstraints() {
    return delegate.getConsumersWorkloadConstraints();
  }

  @Override
  public void createConstraints(TopicName topicName, Constraints constraints) {
    LoggingContext.runWithLogging(
        TOPIC_NAME,
        topicName.qualifiedName(),
        () -> delegate.createConstraints(topicName, constraints));
  }

  @Override
  public void createConstraints(SubscriptionName subscriptionName, Constraints constraints) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.createConstraints(subscriptionName, constraints));
  }

  @Override
  public void updateConstraints(TopicName topicName, Constraints constraints) {
    LoggingContext.runWithLogging(
        TOPIC_NAME,
        topicName.qualifiedName(),
        () -> delegate.updateConstraints(topicName, constraints));
  }

  @Override
  public void updateConstraints(SubscriptionName subscriptionName, Constraints constraints) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.updateConstraints(subscriptionName, constraints));
  }

  @Override
  public void deleteConstraints(TopicName topicName) {
    LoggingContext.runWithLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.deleteConstraints(topicName));
  }

  @Override
  public void deleteConstraints(SubscriptionName subscriptionName) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.deleteConstraints(subscriptionName));
  }

  @Override
  public boolean constraintsExist(TopicName topicName) {
    return LoggingContext.withLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.constraintsExist(topicName));
  }

  @Override
  public boolean constraintsExist(SubscriptionName subscriptionName) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.constraintsExist(subscriptionName));
  }
}
