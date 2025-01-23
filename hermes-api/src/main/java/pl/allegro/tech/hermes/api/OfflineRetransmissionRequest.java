package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = OfflineRetransmissionFromViewRequest.class, name = "view"),
  @JsonSubTypes.Type(value = OfflineRetransmissionFromTopicRequest.class, name = "topic")
})
public sealed class OfflineRetransmissionRequest
    permits OfflineRetransmissionFromTopicRequest, OfflineRetransmissionFromViewRequest {

  private static final List<DateTimeFormatter> formatters =
      List.of(
          DateTimeFormatter.ISO_INSTANT,
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC")),
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").withZone(ZoneId.of("UTC")));
  private static final Logger logger = LoggerFactory.getLogger(OfflineRetransmissionRequest.class);

  private final RetransmissionType type;
  @NotEmpty private final String targetTopic;

  public OfflineRetransmissionRequest(RetransmissionType type, String targetTopic) {
    this.type = type;
    this.targetTopic = targetTopic;
  }

  public RetransmissionType getType() {
    return type;
  }

  public String getTargetTopic() {
    return targetTopic;
  }

  public enum RetransmissionType {
    VIEW,
    TOPIC
  }

  public static Instant initializeTimestamp(String timestamp) {
    if (timestamp == null) {
      return null;
    }

    for (DateTimeFormatter formatter : formatters) {
      try {
        return formatter.parse(timestamp, Instant::from);
      } catch (DateTimeParseException e) {
        // ignore
      }
    }

    logger.warn("Provided date [{}] has an invalid format", timestamp);
    return null;
  }
}
