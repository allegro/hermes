package pl.allegro.tech.hermes.management.domain.subscription;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.query.Query;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

import java.util.List;
import java.util.Optional;

@Component
public class SubscriptionService {

    private static final int LAST_MESSAGE_COUNT = 100;

    private final SubscriptionRepository subscriptionRepository;

    private final SubscriptionMetricsRepository metricsRepository;

    private final UndeliveredMessageLog undeliveredMessageLog;

    private final LogRepository logRepository;

    private final ApiPreconditions preconditions;
    private final AdminTool adminTool;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionMetricsRepository metricsRepository,
                               UndeliveredMessageLog undeliveredMessageLog,
                               LogRepository logRepository,
                               ApiPreconditions apiPreconditions,
                               AdminTool adminTool) {
        this.subscriptionRepository = subscriptionRepository;
        this.metricsRepository = metricsRepository;
        this.undeliveredMessageLog = undeliveredMessageLog;
        this.logRepository = logRepository;
        this.preconditions = apiPreconditions;
        this.adminTool = adminTool;
    }

    public List<String> listSubscriptionNames(TopicName topicName) {
        return subscriptionRepository.listSubscriptionNames(topicName);
    }

    public List<String> listTrackedSubscriptionNames(TopicName topicName) {
        return subscriptionRepository.listTrackedSubscriptionNames(topicName);
    }

    public List<String> listFilteredSubscriptionNames(TopicName topicName, Query<Subscription> query) {
        return subscriptionRepository.listFilteredSubscriptionNames(topicName, query);
    }

    public List<Subscription> listSubscriptions(TopicName topicName) {
        return subscriptionRepository.listSubscriptions(topicName);
    }

    public void createSubscription(Subscription subscription) {
        subscriptionRepository.createSubscription(subscription);
    }

    public Subscription getSubscriptionDetails(TopicName topicName, String subscriptionName) {
        return subscriptionRepository.getSubscriptionDetails(topicName, subscriptionName).anonymizePassword();
    }

    public void removeSubscription(TopicName topicName, String subscriptionName) {
        subscriptionRepository.removeSubscription(topicName, subscriptionName);
    }

    public void updateSubscription(Subscription subscription) {
        Subscription retrieved = subscriptionRepository.getSubscriptionDetails(
                subscription.getTopicName(), subscription.getName()
        );

        Subscription updated = Patch.apply(retrieved, subscription);
        preconditions.checkConstraints(updated);

        if (!retrieved.equals(updated)) {
            subscriptionRepository.updateSubscription(updated);
        }

        if (isConsumerRestartNeeded(retrieved, subscription)) {
            adminTool.restartConsumer(new SubscriptionName(subscription.getName(), subscription.getTopicName()));
        }
    }

    public void updateSubscriptionState(TopicName topicName, String subscriptionName, Subscription.State state) {
        subscriptionRepository.updateSubscriptionState(topicName, subscriptionName, state);
    }

    public Subscription.State getSubscriptionState(TopicName topicName, String subscriptionName) {
        return getSubscriptionDetails(topicName, subscriptionName).getState();
    }

    public SubscriptionMetrics getSubscriptionMetrics(TopicName topicName, String subscriptionName) {
        subscriptionRepository.ensureSubscriptionExists(topicName, subscriptionName);
        return metricsRepository.loadMetrics(topicName, subscriptionName);
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

    private boolean isConsumerRestartNeeded(Subscription retrieved, Subscription subscription) {
        return !retrieved.getEndpoint().equals(subscription.getEndpoint()) ||
               !retrieved.getContentType().equals(subscription.getContentType());
    }
}
