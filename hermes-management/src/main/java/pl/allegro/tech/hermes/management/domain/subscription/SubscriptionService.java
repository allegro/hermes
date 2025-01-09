package pl.allegro.tech.hermes.management.domain.subscription;

import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.api.SubscriptionHealth.Status;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ContentType;
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
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.UnhealthySubscription;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.common.message.undelivered.LastUndeliveredMessageReader;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager;
import pl.allegro.tech.hermes.management.domain.subscription.commands.CreateSubscriptionRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.subscription.commands.UpdateSubscriptionRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthChecker;
import pl.allegro.tech.hermes.management.domain.subscription.validator.SubscriptionValidator;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MovingSubscriptionOffsetsValidationException;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCOffsetChangeSummary;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

public class SubscriptionService {
  private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

  private static final int LAST_MESSAGE_COUNT = 100;

  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionOwnerCache subscriptionOwnerCache;
  private final TopicService topicService;
  private final SubscriptionMetricsRepository metricsRepository;
  private final SubscriptionHealthChecker subscriptionHealthChecker;
  private final LogRepository logRepository;
  private final SubscriptionValidator subscriptionValidator;
  private final Auditor auditor;
  private final MultiDatacenterRepositoryCommandExecutor multiDcExecutor;
  private final MultiDCAwareService multiDCAwareService;
  private final RepositoryManager repositoryManager;
  private final long subscriptionHealthCheckTimeoutMillis;
  private final ExecutorService subscriptionHealthCheckExecutorService;
  private final SubscriptionRemover subscriptionRemover;

  public SubscriptionService(
      SubscriptionRepository subscriptionRepository,
      SubscriptionOwnerCache subscriptionOwnerCache,
      TopicService topicService,
      SubscriptionMetricsRepository metricsRepository,
      SubscriptionHealthChecker subscriptionHealthChecker,
      LogRepository logRepository,
      SubscriptionValidator subscriptionValidator,
      Auditor auditor,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor,
      MultiDCAwareService multiDCAwareService,
      RepositoryManager repositoryManager,
      ExecutorService unhealthyGetExecutorService,
      long unhealthyGetTimeoutMillis,
      SubscriptionRemover subscriptionRemover) {
    this.subscriptionRepository = subscriptionRepository;
    this.subscriptionOwnerCache = subscriptionOwnerCache;
    this.topicService = topicService;
    this.metricsRepository = metricsRepository;
    this.subscriptionHealthChecker = subscriptionHealthChecker;
    this.logRepository = logRepository;
    this.subscriptionValidator = subscriptionValidator;
    this.auditor = auditor;
    this.multiDcExecutor = multiDcExecutor;
    this.multiDCAwareService = multiDCAwareService;
    this.repositoryManager = repositoryManager;
    this.subscriptionHealthCheckExecutorService = unhealthyGetExecutorService;
    this.subscriptionHealthCheckTimeoutMillis = unhealthyGetTimeoutMillis;
    this.subscriptionRemover = subscriptionRemover;
  }

  public List<String> listSubscriptionNames(TopicName topicName) {
    return subscriptionRepository.listSubscriptionNames(topicName);
  }

  public List<String> listTrackedSubscriptionNames(TopicName topicName) {
    return listSubscriptions(topicName).stream()
        .filter(Subscription::isTrackingEnabled)
        .map(Subscription::getName)
        .collect(toList());
  }

  public List<String> listFilteredSubscriptionNames(
      TopicName topicName, Query<Subscription> query) {
    return query.filter(listSubscriptions(topicName)).map(Subscription::getName).collect(toList());
  }

  public List<Subscription> listSubscriptions(TopicName topicName) {
    return subscriptionRepository.listSubscriptions(topicName);
  }

  public void createSubscription(
      Subscription subscription, RequestUser createdBy, String qualifiedTopicName) {
    auditor.beforeObjectCreation(createdBy.getUsername(), subscription);
    subscriptionValidator.checkCreation(subscription, createdBy);

    Topic topic = topicService.getTopicDetails(fromQualifiedName(qualifiedTopicName));
    multiDCAwareService.createConsumerGroups(topic, subscription);

    multiDcExecutor.executeByUser(new CreateSubscriptionRepositoryCommand(subscription), createdBy);
    auditor.objectCreated(createdBy.getUsername(), subscription);
    subscriptionOwnerCache.onCreatedSubscription(subscription);
  }

  public Subscription getSubscriptionDetails(TopicName topicName, String subscriptionName) {
    Subscription subscription =
        subscriptionRepository.getSubscriptionDetails(topicName, subscriptionName).anonymize();
    subscription.setState(getEffectiveState(topicName, subscriptionName));
    return subscription;
  }

  private CompletableFuture<List<Subscription>> getSubscriptionDetails(
      Collection<SubscriptionName> subscriptionNames) {
    return CompletableFuture.supplyAsync(
        () -> subscriptionRepository.getSubscriptionDetails(subscriptionNames),
        subscriptionHealthCheckExecutorService);
  }

  private Subscription.State getEffectiveState(TopicName topicName, String subscriptionName) {
    Set<Subscription.State> states = loadSubscriptionStatesFromAllDc(topicName, subscriptionName);

    if (states.size() > 1) {
      logger.warn("Some states are out of sync: {}", states);
    }

    if (states.contains(Subscription.State.ACTIVE)) {
      return Subscription.State.ACTIVE;
    } else if (states.contains(Subscription.State.SUSPENDED)) {
      return Subscription.State.SUSPENDED;
    } else {
      return Subscription.State.PENDING;
    }
  }

  private Set<Subscription.State> loadSubscriptionStatesFromAllDc(
      TopicName topicName, String subscriptionName) {
    List<DatacenterBoundRepositoryHolder<SubscriptionRepository>> holders =
        repositoryManager.getRepositories(SubscriptionRepository.class);
    Set<Subscription.State> states = new HashSet<>();
    for (DatacenterBoundRepositoryHolder<SubscriptionRepository> holder : holders) {
      try {
        Subscription.State state =
            holder.getRepository().getSubscriptionDetails(topicName, subscriptionName).getState();
        states.add(state);
      } catch (Exception e) {
        logger.warn(
            "Could not load state of subscription (topic: {}, name: {}) from DC {}.",
            topicName,
            subscriptionName,
            holder.getDatacenterName());
      }
    }
    return states;
  }

  public void removeSubscription(
      TopicName topicName, String subscriptionName, RequestUser removedBy) {
    subscriptionRemover.removeSubscription(topicName, subscriptionName, removedBy);
  }

  public void updateSubscription(
      TopicName topicName, String subscriptionName, PatchData patch, RequestUser modifiedBy) {
    auditor.beforeObjectUpdate(
        modifiedBy.getUsername(),
        Subscription.class.getSimpleName(),
        new SubscriptionName(subscriptionName, topicName),
        patch);

    Subscription retrieved =
        subscriptionRepository.getSubscriptionDetails(topicName, subscriptionName);
    Subscription.State oldState = retrieved.getState();
    Subscription updated = Patch.apply(retrieved, patch);
    revertStateIfChangedToPending(updated, oldState);
    subscriptionValidator.checkModification(updated, modifiedBy, retrieved);
    subscriptionOwnerCache.onUpdatedSubscription(retrieved, updated);

    if (!retrieved.equals(updated)) {
      multiDcExecutor.executeByUser(new UpdateSubscriptionRepositoryCommand(updated), modifiedBy);
      auditor.objectUpdated(modifiedBy.getUsername(), retrieved, updated);
    }
  }

  private void revertStateIfChangedToPending(Subscription updated, Subscription.State oldState) {
    if (updated.getState() == Subscription.State.PENDING) {
      updated.setState(oldState);
    }
  }

  public void updateSubscriptionState(
      TopicName topicName,
      String subscriptionName,
      Subscription.State state,
      RequestUser modifiedBy) {
    if (state != Subscription.State.PENDING) {
      PatchData patchData = PatchData.patchData().set("state", state).build();
      auditor.beforeObjectUpdate(
          modifiedBy.getUsername(),
          Subscription.class.getSimpleName(),
          new SubscriptionName(subscriptionName, topicName),
          patchData);
      Subscription retrieved =
          subscriptionRepository.getSubscriptionDetails(topicName, subscriptionName);
      if (!retrieved.getState().equals(state)) {
        Subscription updated = Patch.apply(retrieved, patchData);
        multiDcExecutor.executeByUser(new UpdateSubscriptionRepositoryCommand(updated), modifiedBy);
        auditor.objectUpdated(modifiedBy.getUsername(), retrieved, updated);
      }
    }
  }

  public Subscription.State getSubscriptionState(TopicName topicName, String subscriptionName) {
    return getSubscriptionDetails(topicName, subscriptionName).getState();
  }

  public SubscriptionMetrics getSubscriptionMetrics(TopicName topicName, String subscriptionName) {
    subscriptionRepository.ensureSubscriptionExists(topicName, subscriptionName);
    return metricsRepository.loadMetrics(topicName, subscriptionName);
  }

  public PersistentSubscriptionMetrics getPersistentSubscriptionMetrics(
      TopicName topicName, String subscriptionName) {
    subscriptionRepository.ensureSubscriptionExists(topicName, subscriptionName);
    return metricsRepository.loadZookeeperMetrics(topicName, subscriptionName);
  }

  public SubscriptionHealth getSubscriptionHealth(TopicName topicName, String subscriptionName) {
    Subscription subscription = getSubscriptionDetails(topicName, subscriptionName);
    return getHealth(subscription);
  }

  public Optional<SentMessageTrace> getLatestUndeliveredMessage(
      TopicName topicName, String subscriptionName) {
    List<DatacenterBoundRepositoryHolder<LastUndeliveredMessageReader>> holders =
        repositoryManager.getRepositories(LastUndeliveredMessageReader.class);
    List<SentMessageTrace> traces = new ArrayList<>();
    for (DatacenterBoundRepositoryHolder<LastUndeliveredMessageReader> holder : holders) {
      try {
        holder.getRepository().last(topicName, subscriptionName).ifPresent(traces::add);
      } catch (Exception e) {
        logger.warn(
            "Could not load latest undelivered message from DC: {}", holder.getDatacenterName());
      }
    }
    return traces.stream().max(Comparator.comparing(SentMessageTrace::getTimestamp));
  }

  public List<SentMessageTrace> getLatestUndeliveredMessagesTrackerLogs(
      TopicName topicName, String subscriptionName) {
    return logRepository.getLastUndeliveredMessages(
        topicName.qualifiedName(), subscriptionName, LAST_MESSAGE_COUNT);
  }

  public List<MessageTrace> getMessageStatus(
      String qualifiedTopicName, String subscriptionName, String messageId) {
    return logRepository.getMessageStatus(qualifiedTopicName, subscriptionName, messageId);
  }

  public List<Subscription> querySubscription(Query<Subscription> query) {
    return query.filter(getAllSubscriptions()).collect(toList());
  }

  public List<SubscriptionNameWithMetrics> querySubscriptionsMetrics(
      Query<SubscriptionNameWithMetrics> query) {
    List<Subscription> filteredSubscriptions =
        query.filterNames(getAllSubscriptions()).collect(toList());
    return query.filter(getSubscriptionsMetrics(filteredSubscriptions)).collect(toList());
  }

  public List<Subscription> getAllSubscriptions() {
    return topicService.getAllTopics().stream()
        .map(Topic::getName)
        .map(this::listSubscriptions)
        .flatMap(List::stream)
        .map(Subscription::anonymize)
        .collect(toList());
  }

  public List<Subscription> getForOwnerId(OwnerId ownerId) {
    Collection<SubscriptionName> subscriptionNames = subscriptionOwnerCache.get(ownerId);
    return subscriptionRepository.getSubscriptionDetails(subscriptionNames);
  }

  public List<UnhealthySubscription> getAllUnhealthy(
      boolean respectMonitoringSeverity,
      List<String> subscriptionNames,
      List<String> qualifiedTopicNames) {
    return getUnhealthyList(
        subscriptionOwnerCache.getAll(),
        respectMonitoringSeverity,
        subscriptionNames,
        qualifiedTopicNames);
  }

  public List<UnhealthySubscription> getUnhealthyForOwner(
      OwnerId ownerId,
      boolean respectMonitoringSeverity,
      List<String> subscriptionNames,
      List<String> qualifiedTopicNames) {
    return getUnhealthyList(
        subscriptionOwnerCache.get(ownerId),
        respectMonitoringSeverity,
        subscriptionNames,
        qualifiedTopicNames);
  }

  public SubscriptionStats getStats() {
    List<Subscription> subscriptions = getAllSubscriptions();
    long trackingEnabledSubscriptionsCount =
        subscriptions.stream().filter(Subscription::isTrackingEnabled).count();
    long avroSubscriptionCount =
        subscriptions.stream().filter(s -> s.getContentType() == ContentType.AVRO).count();
    return new SubscriptionStats(
        subscriptions.size(), trackingEnabledSubscriptionsCount, avroSubscriptionCount);
  }

  private List<UnhealthySubscription> getUnhealthyList(
      Collection<SubscriptionName> ownerSubscriptionNames,
      boolean respectMonitoringSeverity,
      List<String> subscriptionNames,
      List<String> qualifiedTopicNames) {
    try {
      return getSubscriptionDetails(ownerSubscriptionNames)
          .thenComposeAsync(
              ownerSubscriptions -> {
                List<CompletableFuture<UnhealthySubscription>> unhealthySubscriptions =
                    filterSubscriptions(
                        ownerSubscriptions,
                        respectMonitoringSeverity,
                        subscriptionNames,
                        qualifiedTopicNames);
                return CompletableFuture.allOf(
                        unhealthySubscriptions.toArray(new CompletableFuture[0]))
                    .thenApply(
                        v ->
                            unhealthySubscriptions.stream()
                                .map(CompletableFuture::join)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()));
              },
              subscriptionHealthCheckExecutorService)
          .get(subscriptionHealthCheckTimeoutMillis, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      logger.error("Timeout occurred while fetching unhealthy subscriptions...", e);
      throw new UnhealthySubscriptionGetException("Fetching unhealthy subscriptions timed out.");
    } catch (Exception e) {
      logger.error("Fetching unhealthy subscriptions failed...", e);
      throw new UnhealthySubscriptionGetException("Fetching unhealthy subscriptions failed.", e);
    }
  }

  public boolean subscriptionExists(SubscriptionName subscriptionName) {
    return subscriptionRepository.subscriptionExists(
        subscriptionName.getTopicName(), subscriptionName.getName());
  }

  private List<CompletableFuture<UnhealthySubscription>> filterSubscriptions(
      Collection<Subscription> subscriptions,
      boolean respectMonitoringSeverity,
      List<String> subscriptionNames,
      List<String> qualifiedTopicNames) {
    boolean shouldFilterBySubscriptionNames = CollectionUtils.isNotEmpty(subscriptionNames);
    boolean shouldFilterByQualifiedTopicNames = CollectionUtils.isNotEmpty(qualifiedTopicNames);

    Stream<Subscription> subscriptionStream =
        subscriptions.stream()
            .filter(
                s ->
                    filterBySeverityMonitorFlag(
                        respectMonitoringSeverity, s.isSeverityNotImportant()));

    if (shouldFilterBySubscriptionNames) {
      subscriptionStream =
          subscriptionStream.filter(s -> filterBySubscriptionNames(subscriptionNames, s.getName()));
    }
    if (shouldFilterByQualifiedTopicNames) {
      subscriptionStream =
          subscriptionStream.filter(
              s -> filterByQualifiedTopicNames(qualifiedTopicNames, s.getQualifiedTopicName()));
    }

    return subscriptionStream
        .map(
            s ->
                CompletableFuture.supplyAsync(
                    () -> getUnhealthy(s), subscriptionHealthCheckExecutorService))
        .collect(toList());
  }

  private boolean filterBySubscriptionNames(
      List<String> subscriptionNames, String subscriptionName) {
    return subscriptionNames.contains(subscriptionName);
  }

  private boolean filterByQualifiedTopicNames(
      List<String> qualifiedTopicNames, String qualifiedTopicName) {
    return qualifiedTopicNames.contains(qualifiedTopicName);
  }

  private boolean filterBySeverityMonitorFlag(
      boolean respectMonitoringSeverity, boolean isSeverityNotImportant) {
    return !(respectMonitoringSeverity && isSeverityNotImportant);
  }

  private SubscriptionHealth getHealth(Subscription subscription) {
    TopicName topicName = subscription.getTopicName();
    TopicMetrics topicMetrics = topicService.getTopicMetrics(topicName);
    SubscriptionMetrics subscriptionMetrics =
        getSubscriptionMetrics(topicName, subscription.getName());
    return subscriptionHealthChecker.checkHealth(subscription, topicMetrics, subscriptionMetrics);
  }

  private UnhealthySubscription getUnhealthy(Subscription subscription) {
    SubscriptionHealth subscriptionHealth = getHealth(subscription);
    if (subscriptionHealth.getStatus() == Status.UNHEALTHY) {
      return UnhealthySubscription.from(subscription, subscriptionHealth);
    } else {
      return null;
    }
  }

  private List<SubscriptionNameWithMetrics> getSubscriptionsMetrics(
      List<Subscription> subscriptions) {
    return subscriptions.stream()
        .map(
            s -> {
              SubscriptionMetrics metrics =
                  metricsRepository.loadMetrics(s.getTopicName(), s.getName());
              return SubscriptionNameWithMetrics.from(
                  metrics, s.getName(), s.getQualifiedTopicName());
            })
        .collect(toList());
  }

  public MultiDCOffsetChangeSummary retransmit(
      Topic topic, String subscriptionName, Long timestamp, boolean dryRun, RequestUser requester) {
    Subscription subscription = getSubscriptionDetails(topic.getName(), subscriptionName);

    MultiDCOffsetChangeSummary multiDCOffsetChangeSummary =
        multiDCAwareService.fetchTopicOffsetsAt(topic, timestamp);

    if (dryRun) return multiDCOffsetChangeSummary;

    /*
     * The subscription state is used to determine how to move the offsets.
     * When the subscription is ACTIVE, the management instance notifies consumers to change offsets.
     * The consumers are responsible for moving their local offsets(KafkaConsumer::seek method) as well as committed ones on Kafka (KafkaConsumer::commitSync method).
     * When the subscription is SUSPENDED, the management instance changes the commited offsets on kafka on its own (AdminClient::alterConsumerGroupOffsets).
     * There is no active consumer to notify in that case.
     */
    switch (subscription.getState()) {
      case ACTIVE:
        multiDCAwareService.moveOffsetsForActiveConsumers(
            topic,
            subscriptionName,
            multiDCOffsetChangeSummary.getPartitionOffsetListPerBrokerName(),
            requester);
        break;
      case SUSPENDED:
        multiDCAwareService.moveOffsets(
            topic,
            subscriptionName,
            multiDCOffsetChangeSummary.getPartitionOffsetListPerBrokerName());
        break;
      case PENDING:
        throw new MovingSubscriptionOffsetsValidationException(
            "Cannot retransmit messages for subscription in PENDING state");
    }

    return multiDCOffsetChangeSummary;
  }
}
