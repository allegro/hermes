package pl.allegro.tech.hermes.management.domain.detection;

import java.time.Duration;
import java.util.Set;

public interface InactiveTopicsDetectionParameters {

  Duration inactivityThreshold();

  Duration nextNotificationThreshold();

  Set<String> whitelistedQualifiedTopicNames();

  int notificationsHistoryLimit();
}
