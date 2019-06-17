package pl.allegro.tech.hermes.management.domain.subscription;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.SubscriptionNameWithMetrics;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.UnhealthySubscription;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthChecker;
import pl.allegro.tech.hermes.management.domain.subscription.validator.SubscriptionValidator;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static pl.allegro.tech.hermes.api.SubscriptionHealth.Status;

@Component
public class SubscriptionService {

    private static final int LAST_MESSAGE_COUNT = 100;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionOwnerCache subscriptionOwnerCache;
    private final TopicService topicService;
    private final SubscriptionMetricsRepository metricsRepository;
    private final SubscriptionHealthChecker subscriptionHealthChecker;
    private final UndeliveredMessageLog undeliveredMessageLog;
    private final LogRepository logRepository;
    private final SubscriptionValidator subscriptionValidator;
    private final Auditor auditor;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionOwnerCache subscriptionOwnerCache,
                               TopicService topicService,
                               SubscriptionMetricsRepository metricsRepository,
                               SubscriptionHealthChecker subscriptionHealthChecker,
                               UndeliveredMessageLog undeliveredMessageLog,
                               LogRepository logRepository,
                               SubscriptionValidator subscriptionValidator,
                               Auditor auditor) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionOwnerCache = subscriptionOwnerCache;
        this.topicService = topicService;
        this.metricsRepository = metricsRepository;
        this.subscriptionHealthChecker = subscriptionHealthChecker;
        this.undeliveredMessageLog = undeliveredMessageLog;
        this.logRepository = logRepository;
        this.subscriptionValidator = subscriptionValidator;
        this.auditor = auditor;
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

    public List<String> listFilteredSubscriptionNames(TopicName topicName, Query<Subscription> query) {
        return query.filter(listSubscriptions(topicName))
                .map(Subscription::getName)
                .collect(toList());
    }

    public List<Subscription> listSubscriptions(TopicName topicName) {
        return subscriptionRepository.listSubscriptions(topicName);
    }

    public void createSubscription(Subscription subscription, String createdBy, CreatorRights creatorRights) {
        subscriptionValidator.checkCreation(subscription, creatorRights);
        subscriptionRepository.createSubscription(subscription);
        auditor.objectCreated(createdBy, subscription);
        subscriptionOwnerCache.onCreatedSubscription(subscription);
    }

    public Subscription getSubscriptionDetails(TopicName topicName, String subscriptionName) {
        return subscriptionRepository.getSubscriptionDetails(topicName, subscriptionName).anonymize();
    }

    public void removeSubscription(TopicName topicName, String subscriptionName, String removedBy) {
        subscriptionRepository.removeSubscription(topicName, subscriptionName);
        auditor.objectRemoved(removedBy, Subscription.class.getSimpleName(), subscriptionName);
        subscriptionOwnerCache.onRemovedSubscription(subscriptionName, topicName);
    }

    public void updateSubscription(TopicName topicName,
                                   String subscriptionName,
                                   PatchData patch,
                                   String modifiedBy) {
        Subscription retrieved = subscriptionRepository.getSubscriptionDetails(topicName, subscriptionName);
        Subscription.State oldState = retrieved.getState();
        Subscription updated = Patch.apply(retrieved, patch);
        revertStateIfChangedToPending(updated, oldState);
        subscriptionValidator.checkModification(updated);
        subscriptionOwnerCache.onUpdatedSubscription(retrieved, updated);

        if (!retrieved.equals(updated)) {
            subscriptionRepository.updateSubscription(updated);
            auditor.objectUpdated(modifiedBy, retrieved, updated);
        }
    }

    private void revertStateIfChangedToPending(Subscription updated, Subscription.State oldState) {
        if (updated.getState() == Subscription.State.PENDING) {
            updated.setState(oldState);
        }
    }

    public void updateSubscriptionState(TopicName topicName, String subscriptionName, Subscription.State state, String modifiedBy) {
        Subscription retrieved = subscriptionRepository.getSubscriptionDetails(topicName, subscriptionName);
        if (state != Subscription.State.PENDING && !retrieved.getState().equals(state)) {
            Subscription updated = Patch.apply(retrieved, PatchData.patchData().set("state", state).build());
            subscriptionRepository.updateSubscription(updated);
            auditor.objectUpdated(modifiedBy, retrieved, updated);
        }
    }

    public Subscription.State getSubscriptionState(TopicName topicName, String subscriptionName) {
        return getSubscriptionDetails(topicName, subscriptionName).getState();
    }

    public SubscriptionMetrics getSubscriptionMetrics(TopicName topicName, String subscriptionName) {
        subscriptionRepository.ensureSubscriptionExists(topicName, subscriptionName);
        return metricsRepository.loadMetrics(topicName, subscriptionName);
    }

    public SubscriptionHealth getSubscriptionHealth(TopicName topicName, String subscriptionName) {
        Subscription subscription = getSubscriptionDetails(topicName, subscriptionName);
        return getHealth(subscription);
    }

    public Optional<SentMessageTrace> getLatestUndeliveredMessage(TopicName topicName, String subscriptionName) {
        return undeliveredMessageLog.last(topicName, subscriptionName);
    }

    public List<SentMessageTrace> getLatestUndeliveredMessagesTrackerLogs(TopicName topicName, String subscriptionName) {
        return logRepository.getLastUndeliveredMessages(topicName.qualifiedName(), subscriptionName, LAST_MESSAGE_COUNT);
    }

    public List<MessageTrace> getMessageStatus(String qualifiedTopicName, String subscriptionName, String messageId) {
        return logRepository.getMessageStatus(qualifiedTopicName, subscriptionName, messageId);
    }

    public List<Subscription> querySubscription(Query<Subscription> query) {
        return query
                .filter(getAllSubscriptions())
                .collect(toList());
    }

    public List<SubscriptionNameWithMetrics> querySubscriptionsMetrics(Query<SubscriptionNameWithMetrics> query) {
        return query.filter(getSubscriptionsMetrics())
                .collect(toList());
    }

    public List<Subscription> getAllSubscriptions() {
        return topicService.getAllTopics()
                .stream()
                .map(Topic::getName)
                .map(this::listSubscriptions)
                .flatMap(List::stream)
                .collect(toList());
    }

    public List<Subscription> getForOwnerId(OwnerId ownerId) {
        Collection<SubscriptionName> subscriptionNames = subscriptionOwnerCache.get(ownerId);
        return subscriptionRepository.getSubscriptionDetails(subscriptionNames);
    }

    public List<UnhealthySubscription> getAllUnhealthy(boolean respectMonitoringSeverity) {
        Collection<SubscriptionName> subscriptionNames = subscriptionOwnerCache.getAll();
        List<Subscription> subscriptions = subscriptionRepository.getSubscriptionDetails(subscriptionNames);
        return filterHealthy(subscriptions, respectMonitoringSeverity);
    }

    public List<UnhealthySubscription> getUnhealthyForOwner(OwnerId ownerId, boolean respectMonitoringSeverity) {
        List<Subscription> ownerSubscriptions = getForOwnerId(ownerId);
        return filterHealthy(ownerSubscriptions, respectMonitoringSeverity);
    }

    private List<UnhealthySubscription> filterHealthy(Collection<Subscription> subscriptions, boolean respectMonitoringSeverity) {
        return subscriptions.stream()
                .filter(s -> filterBySeverityMonitorFlag(respectMonitoringSeverity, s.isSeverityNotImportant()))
                .flatMap(s -> {
                    SubscriptionHealth subscriptionHealth = getHealth(s);

                    if (subscriptionHealth.getStatus() == Status.UNHEALTHY) {
                        return of(UnhealthySubscription.from(s, subscriptionHealth));
                    } else {
                        return empty();
                    }
                })
                .collect(toList());
    }

    private boolean filterBySeverityMonitorFlag(boolean respectMonitoringSeverity, boolean isSeverityNotImportant) {
        return !(respectMonitoringSeverity && isSeverityNotImportant);
    }

    private SubscriptionHealth getHealth(Subscription subscription) {
        TopicName topicName = subscription.getTopicName();
        TopicMetrics topicMetrics = topicService.getTopicMetrics(topicName);
        SubscriptionMetrics subscriptionMetrics = getSubscriptionMetrics(topicName, subscription.getName());
        return subscriptionHealthChecker.checkHealth(subscription, topicMetrics, subscriptionMetrics);
    }

    private List<SubscriptionNameWithMetrics> getSubscriptionsMetrics() {
        return getAllSubscriptions().stream()
                .map(s -> {
                    SubscriptionMetrics metrics = metricsRepository.loadMetrics(s.getTopicName(), s.getName());
                    return SubscriptionNameWithMetrics.from(metrics, s.getName(), s.getQualifiedTopicName());
                }).collect(toList());
    }
}
