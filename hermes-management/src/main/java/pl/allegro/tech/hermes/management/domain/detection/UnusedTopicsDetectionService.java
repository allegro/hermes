package pl.allegro.tech.hermes.management.domain.detection;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.springframework.stereotype.Service;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.config.detection.UnusedTopicsDetectionProperties;

@Service
public class UnusedTopicsDetectionService {
  private final LastPublishedMessageMetricsRepository metricsRepository;
  private final UnusedTopicsDetectionProperties properties;
  private final Clock clock;

  public UnusedTopicsDetectionService(
      LastPublishedMessageMetricsRepository metricsRepository,
      UnusedTopicsDetectionProperties properties,
      Clock clock) {
    this.metricsRepository = metricsRepository;
    this.properties = properties;
    this.clock = clock;
  }

  public Optional<UnusedTopic> detectUnusedTopic(
      TopicName topicName, Optional<UnusedTopic> historicalUnusedTopic) {
    Instant now = clock.instant();
    Instant lastUsed = metricsRepository.getLastPublishedMessageTimestamp(topicName);
    boolean isUnused = isInactive(lastUsed, now);

    if (isUnused) {
      return Optional.of(
          new UnusedTopic(
              topicName.qualifiedName(),
              lastUsed.toEpochMilli(),
              historicalUnusedTopic
                  .map(UnusedTopic::notificationTimestampsMs)
                  .orElse(Collections.emptyList()),
              properties.whitelistedQualifiedTopicNames().contains(topicName.qualifiedName())));
    } else {
      return Optional.empty();
    }
  }

  public boolean shouldBeNotified(UnusedTopic unusedTopic) {
    Instant now = clock.instant();
    Instant lastUsed = Instant.ofEpochMilli(unusedTopic.lastPublishedMessageTimestampMs());
    Optional<Instant> lastNotified =
        unusedTopic.notificationTimestampsMs().stream()
            .max(Long::compare)
            .map(Instant::ofEpochMilli);
    boolean isInactive = isInactive(lastUsed, now);

    return isInactive
        && !unusedTopic.whitelisted()
        && lastNotified.map(instant -> readyForNextNotification(instant, now)).orElse(true);
  }

  private boolean isInactive(Instant lastUsed, Instant now) {
    return Duration.between(lastUsed, now).compareTo(properties.inactivityThreshold()) >= 0;
  }

  private boolean readyForNextNotification(Instant lastNotified, Instant now) {
    return Duration.between(lastNotified, now).compareTo(properties.nextNotificationThreshold())
        >= 0;
  }
}
