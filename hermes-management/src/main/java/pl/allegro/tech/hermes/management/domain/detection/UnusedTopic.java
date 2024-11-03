package pl.allegro.tech.hermes.management.domain.detection;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UnusedTopic(
    @JsonProperty String qualifiedTopicName,
    @JsonProperty long lastPublishedMessageTimestampMs,
    @JsonProperty long lastNotifiedTimestampMs,
    @JsonProperty boolean whitelisted) {}
