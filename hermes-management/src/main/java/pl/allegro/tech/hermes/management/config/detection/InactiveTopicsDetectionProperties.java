package pl.allegro.tech.hermes.management.config.detection;

import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsDetectionParameters;

@ConfigurationProperties(prefix = "management.detection.inactive-topics")
public record InactiveTopicsDetectionProperties(
    Duration inactivityThreshold,
    Duration nextNotificationThreshold,
    Set<String> whitelistedQualifiedTopicNames,
    int notificationsHistoryLimit)
    implements InactiveTopicsDetectionParameters {}
