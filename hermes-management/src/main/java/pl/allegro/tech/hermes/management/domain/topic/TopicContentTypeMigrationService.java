package pl.allegro.tech.hermes.management.domain.topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class TopicContentTypeMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(TopicContentTypeMigrationService.class);

    public static final Duration CHECK_OFFSETS_AVAILABLE_TIMEOUT = Duration.ofSeconds(1);
    public static final Duration INTERVAL_BETWEEN_OFFSETS_AVAILABLE_CHECK_RETRIES = Duration.ofMillis(500);
    public static final Duration INTERVAL_BETWEEN_CHECKING_IF_OFFSETS_MOVED = Duration.ofMillis(1000);
    public static final Duration OFFSETS_MOVED_TIMEOUT = Duration.ofSeconds(10);

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

    public void notifySubscriptions(Topic topic, Instant beforeMigrationInstant) {
        waitUntilOffsetsAvailableOnAllKafkaTopics(topic, CHECK_OFFSETS_AVAILABLE_TIMEOUT);
        subscriptionRepository.listSubscriptionNames(topic.getName()).forEach(s ->
                notifySingleSubscription(topic, beforeMigrationInstant, s)
        );
        subscriptionRepository.listSubscriptionNames(topic.getName()).forEach(s ->
                waitUntilOffsetsMoved(topic, s)
        );
    }

    private void waitUntilOffsetsMoved(Topic topic, String subscriptionName) {
        Instant abortAttemptsInstant = clock.instant().plus(OFFSETS_MOVED_TIMEOUT);

        while(!multiDCAwareService.areOffsetsMoved(topic, subscriptionName)) {
            if (clock.instant().isAfter(abortAttemptsInstant)) {
                throw new UnableToMoveOffsetsException(topic);
            }
            logger.debug("Not all offsets related to hermes topic {} were moved, will retry", topic.getQualifiedName());

            sleep(INTERVAL_BETWEEN_CHECKING_IF_OFFSETS_MOVED);
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
            logger.debug("Not all offsets related to hermes topic {} are available, will retry", topic.getQualifiedName());
            sleep(INTERVAL_BETWEEN_OFFSETS_AVAILABLE_CHECK_RETRIES);
        }
    }

    private void sleep(Duration sleepDuration) {
        try {
            Thread.sleep(sleepDuration.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
