package pl.allegro.tech.hermes.management.domain.detection;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record UnusedTopic(
    @JsonProperty String qualifiedTopicName,
    @JsonProperty long lastPublishedMessageTimestampMs,
    @JsonProperty Optional<Long> lastNotifiedTimestampMs,
    @JsonProperty boolean whitelisted) {}
