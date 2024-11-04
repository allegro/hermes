package pl.allegro.tech.hermes.management.domain.detection;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UnusedTopic(
    @JsonProperty String qualifiedTopicName,
    @JsonProperty long lastPublishedMessageTimestampMs,
    @JsonProperty List<Long> notificationTimestampsMs,
    @JsonProperty boolean whitelisted) {}
