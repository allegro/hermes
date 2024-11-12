package pl.allegro.tech.hermes.management.domain.detection;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record InactiveTopic(
    @JsonProperty String qualifiedTopicName,
    @JsonProperty long lastPublishedMessageTimestampMs,
    @JsonProperty List<Long> notificationTimestampsMs,
    @JsonProperty boolean whitelisted) {

  InactiveTopic notificationSent(Instant timestamp) {
    List<Long> newNotificationTimestampsMs = new ArrayList<>(notificationTimestampsMs);
    newNotificationTimestampsMs.add(timestamp.toEpochMilli());
    return new InactiveTopic(
        this.qualifiedTopicName,
        this.lastPublishedMessageTimestampMs,
        newNotificationTimestampsMs,
        whitelisted);
  }
}
