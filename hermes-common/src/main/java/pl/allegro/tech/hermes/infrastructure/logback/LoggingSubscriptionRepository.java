package pl.allegro.tech.hermes.infrastructure.logback;

import static pl.allegro.tech.hermes.common.logging.LoggingFields.SUBSCRIPTION_NAME;
import static pl.allegro.tech.hermes.common.logging.LoggingFields.TOPIC_NAME;

import java.util.Collection;
import java.util.List;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.logging.LoggingContext;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

public class LoggingSubscriptionRepository implements SubscriptionRepository {

  private final SubscriptionRepository delegate;

  public LoggingSubscriptionRepository(SubscriptionRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean subscriptionExists(TopicName topicName, String subscriptionName) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        new SubscriptionName(subscriptionName, topicName).getQualifiedName(),
        () -> delegate.subscriptionExists(topicName, subscriptionName));
  }

  @Override
  public void ensureSubscriptionExists(TopicName topicName, String subscriptionName) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        new SubscriptionName(subscriptionName, topicName).getQualifiedName(),
        () -> delegate.ensureSubscriptionExists(topicName, subscriptionName));
  }

  @Override
  public void createSubscription(Subscription subscription) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        subscription.getQualifiedName().getQualifiedName(),
        () -> delegate.createSubscription(subscription));
  }

  @Override
  public void removeSubscription(TopicName topicName, String subscriptionName) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        new SubscriptionName(subscriptionName, topicName).getQualifiedName(),
        () -> delegate.removeSubscription(topicName, subscriptionName));
  }

  @Override
  public void updateSubscription(Subscription modifiedSubscription) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        modifiedSubscription.getQualifiedName().getQualifiedName(),
        () -> delegate.updateSubscription(modifiedSubscription));
  }

  @Override
  public void updateSubscriptionState(
      TopicName topicName, String subscriptionName, Subscription.State state) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        new SubscriptionName(subscriptionName, topicName).getQualifiedName(),
        () -> delegate.updateSubscriptionState(topicName, subscriptionName, state));
  }

  @Override
  public Subscription getSubscriptionDetails(TopicName topicName, String subscriptionName) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        new SubscriptionName(subscriptionName, topicName).getQualifiedName(),
        () -> delegate.getSubscriptionDetails(topicName, subscriptionName));
  }

  @Override
  public Subscription getSubscriptionDetails(SubscriptionName subscriptionName) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.getSubscriptionDetails(subscriptionName));
  }

  @Override
  public List<Subscription> getSubscriptionDetails(Collection<SubscriptionName> subscriptionNames) {
    return delegate.getSubscriptionDetails(subscriptionNames);
  }

  @Override
  public List<String> listSubscriptionNames(TopicName topicName) {
    return LoggingContext.withLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.listSubscriptionNames(topicName));
  }

  @Override
  public List<Subscription> listSubscriptions(TopicName topicName) {
    return LoggingContext.withLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.listSubscriptions(topicName));
  }

  @Override
  public List<Subscription> listAllSubscriptions() {
    return delegate.listAllSubscriptions();
  }
}
