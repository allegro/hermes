package pl.allegro.tech.hermes.management.domain.detection;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record InactiveTopic(
    @JsonProperty("topic") String qualifiedTopicName,
    @JsonProperty("lastPublishedTsMs") long lastPublishedMessageTimestampMs,
    @JsonProperty("notificationTsMs") List<Long> notificationTimestampsMs,
    @JsonProperty("whitelisted") boolean whitelisted) {

  InactiveTopic notificationSent(Instant timestamp) {
    List<Long> newNotificationTimestampsMs = new ArrayList<>(notificationTimestampsMs);
    newNotificationTimestampsMs.add(timestamp.toEpochMilli());
    return new InactiveTopic(
        this.qualifiedTopicName,
        this.lastPublishedMessageTimestampMs,
        newNotificationTimestampsMs,
        this.whitelisted);
  }

  InactiveTopic limitNotificationsHistory(int limit) {
    List<Long> newNotificationTimestampsMs =
        notificationTimestampsMs.stream()
            .sorted((a, b) -> Long.compare(b, a))
            .limit(limit)
            .toList();
    return new InactiveTopic(
        this.qualifiedTopicName,
        this.lastPublishedMessageTimestampMs,
        newNotificationTimestampsMs,
        this.whitelisted);
  }
}
