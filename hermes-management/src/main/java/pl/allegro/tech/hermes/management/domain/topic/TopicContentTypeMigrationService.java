package pl.allegro.tech.hermes.management.domain.topic;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class TopicContentTypeMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(TopicContentTypeMigrationService.class);

    private static final Duration CHECK_OFFSETS_AVAILABLE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration INTERVAL_BETWEEN_OFFSETS_AVAILABLE_CHECK = Duration.ofMillis(500);
    private static final Duration INTERVAL_BETWEEN_ASSIGNMENTS_COMPLETED_CHECK = Duration.ofMillis(500);

    private final SubscriptionRepository subscriptionRepository;
    private final MultiDCAwareService multiDCAwareService;
    private final Clock clock;

    @Autowired
    public TopicContentTypeMigrationService(SubscriptionRepository subscriptionRepository,
                                            MultiDCAwareService multiDCAwareService,
                                            Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.multiDCAwareService = multiDCAwareService;
        this.clock = clock;
    }

    void notifySubscriptions(Topic topic, Instant beforeMigrationInstant) {
        waitUntilOffsetsAvailableOnAllKafkaTopics(topic, CHECK_OFFSETS_AVAILABLE_TIMEOUT);
        logger.info("Offsets available on all partitions of topic {}", topic.getQualifiedName());
        notSuspendedSubscriptionsForTopic(topic)
                .map(Subscription::getName)
                .forEach(sub -> notifySingleSubscription(topic, beforeMigrationInstant, sub));
    }

    void waitUntilAllSubscriptionsHasConsumersAssigned(Topic topic, Duration assignmentCompletedTimeout) {
        Instant abortAttemptsInstant = clock.instant().plus(assignmentCompletedTimeout);

        while (!allSubscriptionsHaveConsumersAssigned(topic)) {
            if (clock.instant().isAfter(abortAttemptsInstant)) {
                throw new AssignmentsToSubscriptionsNotCompletedException(topic);
            }
            sleep(INTERVAL_BETWEEN_ASSIGNMENTS_COMPLETED_CHECK);
        }
    }

    private void notifySingleSubscription(Topic topic, Instant beforeMigrationInstant, String subscriptionName) {
        multiDCAwareService.moveOffset(topic, subscriptionName, beforeMigrationInstant.toEpochMilli(), false);
    }

    private void waitUntilOffsetsAvailableOnAllKafkaTopics(Topic topic, Duration offsetsAvailableTimeout) {
        Instant abortAttemptsInstant = clock.instant().plus(offsetsAvailableTimeout);

        while (!multiDCAwareService.areOffsetsAvailableOnAllKafkaTopics(topic)) {
            if (clock.instant().isAfter(abortAttemptsInstant)) {
                throw new OffsetsNotAvailableException(topic);
            }
            logger.info("Not all offsets related to hermes topic {} are available, will retry", topic.getQualifiedName());
            sleep(INTERVAL_BETWEEN_OFFSETS_AVAILABLE_CHECK);
        }
    }

    private void sleep(Duration sleepDuration) {
        try {
            Thread.sleep(sleepDuration.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean allSubscriptionsHaveConsumersAssigned(Topic topic) {
        List<Subscription> notSuspendedSubscriptions = notSuspendedSubscriptionsForTopic(topic)
                .collect(Collectors.toList());
        return multiDCAwareService.allSubscriptionsHaveConsumersAssigned(topic, notSuspendedSubscriptions);
    }

    private Stream<Subscription> notSuspendedSubscriptionsForTopic(Topic topic) {
        return subscriptionRepository.listSubscriptions(topic.getName())
                .stream()
                .filter(sub -> Subscription.State.SUSPENDED != sub.getState());
    }
}
