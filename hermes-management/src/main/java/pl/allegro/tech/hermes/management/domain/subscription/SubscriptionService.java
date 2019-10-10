package pl.allegro.tech.hermes.management.domain.subscription;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager;
import pl.allegro.tech.hermes.management.domain.subscription.commands.CreateSubscriptionRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.subscription.commands.RemoveSubscriptionRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.subscription.commands.UpdateSubscriptionRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthChecker;
import pl.allegro.tech.hermes.management.domain.subscription.validator.SubscriptionValidator;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static pl.allegro.tech.hermes.api.SubscriptionHealth.Status;

@Component
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
    private final RepositoryManager repositoryManager;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionOwnerCache subscriptionOwnerCache,
                               TopicService topicService,
                               SubscriptionMetricsRepository metricsRepository,
                               SubscriptionHealthChecker subscriptionHealthChecker,
                               LogRepository logRepository,
                               SubscriptionValidator subscriptionValidator,
                               Auditor auditor,
                               MultiDatacenterRepositoryCommandExecutor multiDcExecutor,
                               RepositoryManager repositoryManager) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionOwnerCache = subscriptionOwnerCache;
        this.topicService = topicService;
        this.metricsRepository = metricsRepository;
        this.subscriptionHealthChecker = subscriptionHealthChecker;
        this.logRepository = logRepository;
        this.subscriptionValidator = subscriptionValidator;
        this.auditor = auditor;
        this.multiDcExecutor = multiDcExecutor;
        this.repositoryManager = repositoryManager;
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
        multiDcExecutor.execute(new CreateSubscriptionRepositoryCommand(subscription));
        auditor.objectCreated(createdBy, subscription);
        subscriptionOwnerCache.onCreatedSubscription(subscription);
    }

    public Subscription getSubscriptionDetails(TopicName topicName, String subscriptionName) {
        Subscription subscription = subscriptionRepository.getSubscriptionDetails(topicName, subscriptionName)
                .anonymize();
        subscription.setState(getEffectiveState(topicName, subscriptionName));
        return subscription;
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

    private Set<Subscription.State> loadSubscriptionStatesFromAllDc(TopicName topicName, String subscriptionName) {
        List<DatacenterBoundRepositoryHolder<SubscriptionRepository>> holders =
                repositoryManager.getRepositories(SubscriptionRepository.class);
        Set<Subscription.State> states = new HashSet<>();
        for (DatacenterBoundRepositoryHolder<SubscriptionRepository> holder : holders) {
            try {
                Subscription.State state = holder.getRepository().getSubscriptionDetails(topicName, subscriptionName)
                        .getState();
                states.add(state);
            } catch (Exception e) {
                logger.warn("Could not load state of subscription (topic: {}, name: {}) from DC {}.",
                        topicName, subscriptionName, holder.getDatacenterName());
            }
        }
        return states;
    }

    public void removeSubscription(TopicName topicName, String subscriptionName, String removedBy) {
        multiDcExecutor.execute(new RemoveSubscriptionRepositoryCommand(topicName, subscriptionName));
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
            multiDcExecutor.execute(new UpdateSubscriptionRepositoryCommand(updated));
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
            multiDcExecutor.execute(new UpdateSubscriptionRepositoryCommand(updated));
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
        List<DatacenterBoundRepositoryHolder<UndeliveredMessageLog>> holders =
                repositoryManager.getRepositories(UndeliveredMessageLog.class);
        List<SentMessageTrace> traces = new ArrayList<>();
        for (DatacenterBoundRepositoryHolder<UndeliveredMessageLog> holder : holders) {
            try {
                holder.getRepository().last(topicName, subscriptionName).ifPresent(traces::add);
            } catch (Exception e) {
                logger.warn("Could not load latest undelivered message from DC: {}", holder.getDatacenterName());
            }
        }
        return traces.stream().max(Comparator.comparing(SentMessageTrace::getTimestamp));
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

    public List<UnhealthySubscription> getAllUnhealthy(boolean respectMonitoringSeverity, List<String> subscriptionNames,
                                                       List<String> qualifiedTopicNames) {
        List<Subscription> subscriptions = subscriptionRepository.getSubscriptionDetails(subscriptionOwnerCache.getAll());
        return filterSubscriptions(subscriptions, respectMonitoringSeverity, subscriptionNames, qualifiedTopicNames);
    }

    public List<UnhealthySubscription> getUnhealthyForOwner(OwnerId ownerId, boolean respectMonitoringSeverity,
                                                            List<String> subscriptionNames, List<String> qualifiedTopicNames) {
        List<Subscription> ownerSubscriptions = getForOwnerId(ownerId);
        return filterSubscriptions(ownerSubscriptions, respectMonitoringSeverity, subscriptionNames, qualifiedTopicNames);
    }

    private List<UnhealthySubscription> filterSubscriptions(Collection<Subscription> subscriptions, boolean respectMonitoringSeverity,
                                                            List<String> subscriptionNames, List<String> qualifiedTopicNames) {
        boolean shouldFilterBySubscriptionNames = CollectionUtils.isNotEmpty(subscriptionNames);
        boolean shouldFilterByQualifiedTopicNames = CollectionUtils.isNotEmpty(qualifiedTopicNames);

        Stream<Subscription> subscriptionStream = subscriptions.stream()
                .filter(s -> filterBySeverityMonitorFlag(respectMonitoringSeverity, s.isSeverityNotImportant()));

        if (shouldFilterBySubscriptionNames) {
            subscriptionStream = subscriptionStream.filter(s -> filterBySubscriptionNames(subscriptionNames, s.getName()));
        }
        if (shouldFilterByQualifiedTopicNames) {
            subscriptionStream = subscriptionStream.filter(s -> filterByQualifiedTopicNames(qualifiedTopicNames, s.getQualifiedTopicName()));
        }

        return subscriptionStream.flatMap(s -> {
            SubscriptionHealth subscriptionHealth = getHealth(s);
            if (subscriptionHealth.getStatus() == Status.UNHEALTHY) {
                return of(UnhealthySubscription.from(s, subscriptionHealth));
            } else {
                return empty();
            }
        }).collect(toList());
    }

    private boolean filterBySubscriptionNames(List<String> subscriptionNames, String subscriptionName) {
        return subscriptionNames.contains(subscriptionName);
    }

    private boolean filterByQualifiedTopicNames(List<String> qualifiedTopicNames, String qualifiedTopicName) {
        return qualifiedTopicNames.contains(qualifiedTopicName);
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
