package pl.allegro.tech.hermes.management.config.detection;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Set;

@ConfigurationProperties(prefix = "detection.unused-topics")
public record UnusedTopicsDetectionProperties(
    Duration inactivityThreshold,
    Duration nextNotificationThreshold,
    Set<String> whitelistedQualifiedTopicNames,
    String leaderElectionZookeeperDc) {}
