package pl.allegro.tech.hermes.management.domain.subscription;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.*;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthChecker;
import pl.allegro.tech.hermes.management.domain.subscription.validator.SubscriptionValidator;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SubscriptionService {

    private static final int LAST_MESSAGE_COUNT = 100;

    private final SubscriptionRepository subscriptionRepository;
    private final TopicService topicService;
    private final SubscriptionMetricsRepository metricsRepository;
    private final SubscriptionHealthChecker subscriptionHealthChecker;
    private final UndeliveredMessageLog undeliveredMessageLog;
    private final LogRepository logRepository;
    private final SubscriptionValidator subscriptionValidator;
    private final Auditor auditor;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               TopicService topicService,
                               SubscriptionMetricsRepository metricsRepository,
                               SubscriptionHealthChecker subscriptionHealthChecker,
                               UndeliveredMessageLog undeliveredMessageLog,
                               LogRepository logRepository,
                               SubscriptionValidator subscriptionValidator,
                               Auditor auditor) {
        this.subscriptionRepository = subscriptionRepository;
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
                .collect(Collectors.toList());
    }

    public List<String> listFilteredSubscriptionNames(TopicName topicName, Query<Subscription> query) {
        return query.filter(listSubscriptions(topicName))
                .map(Subscription::getName)
                .collect(Collectors.toList());
    }

    public List<Subscription> listSubscriptions(TopicName topicName) {
        return subscriptionRepository.listSubscriptions(topicName);
    }

    public void createSubscription(Subscription subscription, String createdBy) {
        subscriptionValidator.check(subscription);
        subscriptionRepository.createSubscription(subscription);
        auditor.objectCreated(createdBy, subscription);
    }

    public Subscription getSubscriptionDetails(TopicName topicName, String subscriptionName) {
        return subscriptionRepository.getSubscriptionDetails(topicName, subscriptionName).anonymize();
    }

    public void removeSubscription(TopicName topicName, String subscriptionName, String removedBy) {
        subscriptionRepository.removeSubscription(topicName, subscriptionName);
        auditor.objectRemoved(removedBy, Subscription.class.getSimpleName(), subscriptionName);
    }

    public void updateSubscription(TopicName topicName,
                                   String subscriptionName,
                                   PatchData patch,
                                   String modifiedBy) {
        Subscription retrieved = subscriptionRepository.getSubscriptionDetails(topicName, subscriptionName);
        Subscription updated = Patch.apply(retrieved, patch);
        subscriptionValidator.check(updated);

        if (!retrieved.equals(updated)) {
            subscriptionRepository.updateSubscription(updated);
            auditor.objectUpdated(modifiedBy, retrieved, updated);
        }
    }

    public void updateSubscriptionState(TopicName topicName, String subscriptionName, Subscription.State state, String modifiedBy) {
        Subscription retrieved = subscriptionRepository.getSubscriptionDetails(topicName, subscriptionName);
        if (!retrieved.getState().equals(state)) {
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
        TopicMetrics topicMetrics = topicService.getTopicMetrics(topicName);
        SubscriptionMetrics subscriptionMetrics = getSubscriptionMetrics(topicName, subscriptionName);
        return subscriptionHealthChecker.checkHealth(subscription, topicMetrics, subscriptionMetrics);
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
                .collect(Collectors.toList());
    }

    public List<Subscription> getAllSubscriptions() {
        return topicService.getAllTopics()
                .stream()
                .map(Topic::getName)
                .map(this::listSubscriptions)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
