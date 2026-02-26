package pl.allegro.tech.hermes.management.domain.subscription;

import static pl.allegro.tech.hermes.common.logging.LoggingFields.SUBSCRIPTION_NAME;
import static pl.allegro.tech.hermes.common.logging.LoggingFields.TOPIC_NAME;

import java.util.List;
import java.util.Optional;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.PersistentSubscriptionMetrics;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.SubscriptionNameWithMetrics;
import pl.allegro.tech.hermes.api.SubscriptionStats;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.UnhealthySubscription;
import pl.allegro.tech.hermes.common.logging.LoggingContext;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCOffsetChangeSummary;

public class LoggingSubscriptionService implements SubscriptionManagement {

  private final SubscriptionManagement delegate;

  public LoggingSubscriptionService(SubscriptionManagement delegate) {
    this.delegate = delegate;
  }

  @Override
  public List<String> listSubscriptionNames(TopicName topicName) {
    return LoggingContext.withLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.listSubscriptionNames(topicName));
  }

  @Override
  public List<String> listTrackedSubscriptionNames(TopicName topicName) {
    return LoggingContext.withLogging(
        TOPIC_NAME,
        topicName.qualifiedName(),
        () -> delegate.listTrackedSubscriptionNames(topicName));
  }

  @Override
  public List<String> listFilteredSubscriptionNames(
      TopicName topicName, Query<Subscription> query) {
    return LoggingContext.withLogging(
        TOPIC_NAME,
        topicName.qualifiedName(),
        () -> delegate.listFilteredSubscriptionNames(topicName, query));
  }

  @Override
  public List<Subscription> listSubscriptions(TopicName topicName) {
    return LoggingContext.withLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.listSubscriptions(topicName));
  }

  @Override
  public void createSubscription(
      Subscription subscription, RequestUser createdBy, String qualifiedTopicName) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        subscription.getQualifiedName().getQualifiedName(),
        () -> delegate.createSubscription(subscription, createdBy, qualifiedTopicName));
  }

  @Override
  public Subscription getSubscriptionDetails(SubscriptionName subscriptionName) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.getSubscriptionDetails(subscriptionName));
  }

  @Override
  public void removeSubscription(SubscriptionName subscriptionName, RequestUser removedBy) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.removeSubscription(subscriptionName, removedBy));
  }

  @Override
  public void updateSubscription(
      SubscriptionName subscriptionName, PatchData patch, RequestUser modifiedBy) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.updateSubscription(subscriptionName, patch, modifiedBy));
  }

  @Override
  public void updateSubscriptionState(
      SubscriptionName subscriptionName, Subscription.State state, RequestUser modifiedBy) {
    LoggingContext.runWithLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.updateSubscriptionState(subscriptionName, state, modifiedBy));
  }

  @Override
  public Subscription.State getSubscriptionState(SubscriptionName subscriptionName) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.getSubscriptionState(subscriptionName));
  }

  @Override
  public SubscriptionMetrics getSubscriptionMetrics(SubscriptionName subscriptionName) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.getSubscriptionMetrics(subscriptionName));
  }

  @Override
  public PersistentSubscriptionMetrics getPersistentSubscriptionMetrics(
      SubscriptionName subscriptionName) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.getPersistentSubscriptionMetrics(subscriptionName));
  }

  @Override
  public SubscriptionHealth getSubscriptionHealth(SubscriptionName subscriptionName) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.getSubscriptionHealth(subscriptionName));
  }

  @Override
  public Optional<SentMessageTrace> getLatestUndeliveredMessage(SubscriptionName subscriptionName) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.getLatestUndeliveredMessage(subscriptionName));
  }

  @Override
  public List<SentMessageTrace> getLatestUndeliveredMessagesTrackerLogs(
      SubscriptionName subscriptionName) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.getLatestUndeliveredMessagesTrackerLogs(subscriptionName));
  }

  @Override
  public List<MessageTrace> getMessageStatus(SubscriptionName subscriptionName, String messageId) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.getMessageStatus(subscriptionName, messageId));
  }

  @Override
  public List<Subscription> querySubscription(Query<Subscription> query) {
    return delegate.querySubscription(query);
  }

  @Override
  public List<SubscriptionNameWithMetrics> querySubscriptionsMetrics(
      Query<SubscriptionNameWithMetrics> query) {
    return delegate.querySubscriptionsMetrics(query);
  }

  @Override
  public List<Subscription> getAllSubscriptions() {
    return delegate.getAllSubscriptions();
  }

  @Override
  public List<Subscription> getForOwnerId(OwnerId ownerId) {
    return delegate.getForOwnerId(ownerId);
  }

  @Override
  public List<UnhealthySubscription> getAllUnhealthy(
      boolean respectMonitoringSeverity,
      List<String> subscriptionNames,
      List<String> qualifiedTopicNames) {
    return delegate.getAllUnhealthy(
        respectMonitoringSeverity, subscriptionNames, qualifiedTopicNames);
  }

  @Override
  public List<UnhealthySubscription> getUnhealthyForOwner(
      OwnerId ownerId,
      boolean respectMonitoringSeverity,
      List<String> subscriptionNames,
      List<String> qualifiedTopicNames) {
    return delegate.getUnhealthyForOwner(
        ownerId, respectMonitoringSeverity, subscriptionNames, qualifiedTopicNames);
  }

  @Override
  public SubscriptionStats getStats() {
    return delegate.getStats();
  }

  @Override
  public boolean subscriptionExists(SubscriptionName subscriptionName) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.subscriptionExists(subscriptionName));
  }

  @Override
  public MultiDCOffsetChangeSummary retransmit(
      Topic topic,
      SubscriptionName subscriptionName,
      Long timestamp,
      boolean dryRun,
      RequestUser requester) {
    return LoggingContext.withLogging(
        SUBSCRIPTION_NAME,
        subscriptionName.getQualifiedName(),
        () -> delegate.retransmit(topic, subscriptionName, timestamp, dryRun, requester));
  }
}
