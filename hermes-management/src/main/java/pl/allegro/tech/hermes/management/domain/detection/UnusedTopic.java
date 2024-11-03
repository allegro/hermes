package pl.allegro.tech.hermes.management.domain.detection;

import pl.allegro.tech.hermes.api.TopicName;

import java.time.Instant;

public record UnusedTopic(
    TopicName topicName, Instant lastPublishedMessage, Instant lastNotified, boolean whitelisted) {}
