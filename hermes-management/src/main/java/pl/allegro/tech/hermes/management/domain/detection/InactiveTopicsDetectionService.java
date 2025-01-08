package pl.allegro.tech.hermes.management.domain.detection;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.springframework.stereotype.Service;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.config.detection.InactiveTopicsDetectionProperties;

@Service
public class InactiveTopicsDetectionService {
  private final LastPublishedMessageMetricsRepository metricsRepository;
  private final InactiveTopicsDetectionProperties properties;
  private final Clock clock;

  public InactiveTopicsDetectionService(
      LastPublishedMessageMetricsRepository metricsRepository,
      InactiveTopicsDetectionProperties properties,
      Clock clock) {
    this.metricsRepository = metricsRepository;
    this.properties = properties;
    this.clock = clock;
  }

  public Optional<InactiveTopic> detectInactiveTopic(
      TopicName topicName, Optional<InactiveTopic> historicalInactiveTopic) {
    Instant now = clock.instant();
    Optional<Instant> lastUsedOptional =
        metricsRepository.getLastPublishedMessageTimestamp(topicName);
    if (lastUsedOptional.isEmpty()) {
      return Optional.empty();
    }
    Instant lastUsed = lastUsedOptional.get();

    if (isInactive(lastUsed, now)) {
      return Optional.of(
          new InactiveTopic(
              topicName.qualifiedName(),
              lastUsed.toEpochMilli(),
              historicalInactiveTopic
                  .map(InactiveTopic::notificationTimestampsMs)
                  .orElse(Collections.emptyList()),
              properties.whitelistedQualifiedTopicNames().contains(topicName.qualifiedName())));
    } else {
      return Optional.empty();
    }
  }

  public boolean shouldBeNotified(InactiveTopic inactiveTopic) {
    Instant now = clock.instant();
    Instant lastUsed = Instant.ofEpochMilli(inactiveTopic.lastPublishedMessageTimestampMs());
    Optional<Instant> lastNotified =
        inactiveTopic.notificationTimestampsMs().stream()
            .max(Long::compare)
            .map(Instant::ofEpochMilli);
    boolean isInactive = isInactive(lastUsed, now);

    return isInactive
        && !inactiveTopic.whitelisted()
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
