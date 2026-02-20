package pl.allegro.tech.hermes.management.domain.detection;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import pl.allegro.tech.hermes.api.TopicName;

public class InactiveTopicsDetectionService {
  private final LastPublishedMessageMetricsRepository metricsRepository;
  private final InactiveTopicsDetectionParameters parameters;
  private final Clock clock;

  public InactiveTopicsDetectionService(
      LastPublishedMessageMetricsRepository metricsRepository,
      InactiveTopicsDetectionParameters parameters,
      Clock clock) {
    this.metricsRepository = metricsRepository;
    this.parameters = parameters;
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
              parameters.whitelistedQualifiedTopicNames().contains(topicName.qualifiedName())));
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
    return Duration.between(lastUsed, now).compareTo(parameters.inactivityThreshold()) >= 0;
  }

  private boolean readyForNextNotification(Instant lastNotified, Instant now) {
    return Duration.between(lastNotified, now).compareTo(parameters.nextNotificationThreshold())
        >= 0;
  }
}
