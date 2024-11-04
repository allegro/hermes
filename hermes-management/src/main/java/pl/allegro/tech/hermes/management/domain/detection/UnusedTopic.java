package pl.allegro.tech.hermes.management.domain.detection;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record UnusedTopic(
    @JsonProperty String qualifiedTopicName,
    @JsonProperty long lastPublishedMessageTimestampMs,
    @JsonProperty List<Long> notificationTimestampsMs,
    @JsonProperty boolean whitelisted) {

  UnusedTopic notificationSent(Instant timestamp) {
    List<Long> newNotificationTimestampsMs = new ArrayList<>(notificationTimestampsMs);
    newNotificationTimestampsMs.add(timestamp.toEpochMilli());
    return new UnusedTopic(
        this.qualifiedTopicName,
        this.lastPublishedMessageTimestampMs,
        newNotificationTimestampsMs,
        whitelisted);
  }
}
