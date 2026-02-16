package pl.allegro.tech.hermes.management.domain.subscription;

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
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCOffsetChangeSummary;

public interface SubscriptionManagement {

  List<String> listSubscriptionNames(TopicName topicName);

  List<String> listTrackedSubscriptionNames(TopicName topicName);

  List<String> listFilteredSubscriptionNames(TopicName topicName, Query<Subscription> query);

  List<Subscription> listSubscriptions(TopicName topicName);

  void createSubscription(
      Subscription subscription, RequestUser createdBy, String qualifiedTopicName);

  Subscription getSubscriptionDetails(SubscriptionName subscriptionName);

  void removeSubscription(SubscriptionName subscriptionName, RequestUser removedBy);

  void updateSubscription(
      SubscriptionName subscriptionName, PatchData patch, RequestUser modifiedBy);

  void updateSubscriptionState(
      SubscriptionName subscriptionName, Subscription.State state, RequestUser modifiedBy);

  Subscription.State getSubscriptionState(SubscriptionName subscriptionName);

  SubscriptionMetrics getSubscriptionMetrics(SubscriptionName subscriptionName);

  PersistentSubscriptionMetrics getPersistentSubscriptionMetrics(SubscriptionName subscriptionName);

  SubscriptionHealth getSubscriptionHealth(SubscriptionName subscriptionName);

  Optional<SentMessageTrace> getLatestUndeliveredMessage(SubscriptionName subscriptionName);

  List<SentMessageTrace> getLatestUndeliveredMessagesTrackerLogs(SubscriptionName subscriptionName);

  List<MessageTrace> getMessageStatus(SubscriptionName subscriptionName, String messageId);

  List<Subscription> querySubscription(Query<Subscription> query);

  List<SubscriptionNameWithMetrics> querySubscriptionsMetrics(
      Query<SubscriptionNameWithMetrics> query);

  List<Subscription> getAllSubscriptions();

  List<Subscription> getForOwnerId(OwnerId ownerId);

  List<UnhealthySubscription> getAllUnhealthy(
      boolean respectMonitoringSeverity,
      List<String> subscriptionNames,
      List<String> qualifiedTopicNames);

  List<UnhealthySubscription> getUnhealthyForOwner(
      OwnerId ownerId,
      boolean respectMonitoringSeverity,
      List<String> subscriptionNames,
      List<String> qualifiedTopicNames);

  SubscriptionStats getStats();

  boolean subscriptionExists(SubscriptionName subscriptionName);

  MultiDCOffsetChangeSummary retransmit(
      Topic topic,
      SubscriptionName subscriptionName,
      Long timestamp,
      boolean dryRun,
      RequestUser requester);
}
