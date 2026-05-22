package pl.allegro.tech.hermes.consumers.consumer;

import static pl.allegro.tech.hermes.common.logging.LoggingFields.SUBSCRIPTION_NAME;

import java.util.Set;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;
import pl.allegro.tech.hermes.common.logging.LoggingContext;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

public class LoggingConsumer implements Consumer {

  private final Consumer delegate;

  public LoggingConsumer(Consumer delegate) {
    this.delegate = delegate;
  }

  @Override
  public void consume(Runnable signalsInterrupt) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        delegate.getSubscription().getQualifiedName().getQualifiedName(),
        () -> delegate.consume(signalsInterrupt));
  }

  @Override
  public void initialize() {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        delegate.getSubscription().getQualifiedName().getQualifiedName(),
        delegate::initialize);
  }

  @Override
  public void tearDown() {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        delegate.getSubscription().getQualifiedName().getQualifiedName(),
        delegate::tearDown);
  }

  @Override
  public void updateSubscription(Subscription subscription) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        subscription.getQualifiedName().getQualifiedName(),
        () -> delegate.updateSubscription(subscription));
  }

  @Override
  public void updateTopic(Topic topic) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        delegate.getSubscription().getQualifiedName().getQualifiedName(),
        () -> delegate.updateTopic(topic));
  }

  @Override
  public void commit(Set<SubscriptionPartitionOffset> offsets) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        delegate.getSubscription().getQualifiedName().getQualifiedName(),
        () -> delegate.commit(offsets));
  }

  @Override
  public PartitionOffsets moveOffset(PartitionOffsets subscriptionPartitionOffsets) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        delegate.getSubscription().getQualifiedName().getQualifiedName(),
        () -> delegate.moveOffset(subscriptionPartitionOffsets));
  }

  @Override
  public Subscription getSubscription() {
    return delegate.getSubscription();
  }

  @Override
  public Set<Integer> getAssignedPartitions() {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        delegate.getSubscription().getQualifiedName().getQualifiedName(),
        delegate::getAssignedPartitions);
  }
}
