package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.constraints.OneSourceRetransmission;
import pl.allegro.tech.hermes.api.constraints.TimeRangeAbsentForViewRetransmission;
import pl.allegro.tech.hermes.api.constraints.ProperTimeRangePresentForTopicRetransmission;
import pl.allegro.tech.hermes.api.jackson.OptionalInstantIsoSerializer;

@OneSourceRetransmission
@TimeRangeAbsentForViewRetransmission
@ProperTimeRangePresentForTopicRetransmission
public class OfflineRetransmissionRequest {

  private static final List<DateTimeFormatter> formatters =
      List.of(
          DateTimeFormatter.ISO_INSTANT,
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC")),
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").withZone(ZoneId.of("UTC")));
  private static final Logger logger = LoggerFactory.getLogger(OfflineRetransmissionRequest.class);

  private final String sourceViewPath;
  private final String sourceTopic;
  @NotEmpty private final String targetTopic;
  @Nullable private final Instant startTimestamp;
  @Nullable private final Instant endTimestamp;

  @JsonCreator
  public OfflineRetransmissionRequest(
      @JsonProperty("sourceViewPath") String sourceViewPath,
      @JsonProperty("sourceTopic") String sourceTopic,
      @JsonProperty("targetTopic") String targetTopic,
      @JsonProperty("startTimestamp") String startTimestamp,
      @JsonProperty("endTimestamp") String endTimestamp) {
    this.sourceViewPath = sourceViewPath;
    this.sourceTopic = sourceTopic;
    this.targetTopic = targetTopic;
    this.startTimestamp = initializeTimestamp(startTimestamp);
    this.endTimestamp = initializeTimestamp(endTimestamp);
  }

  private Instant initializeTimestamp(String timestamp) {
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

  public Optional<String> getSourceViewPath() {
    return Optional.ofNullable(sourceViewPath);
  }

  public Optional<String> getSourceTopic() {
    return Optional.ofNullable(sourceTopic);
  }

  public String getTargetTopic() {
    return targetTopic;
  }

  @JsonSerialize(using = OptionalInstantIsoSerializer.class)
  public Optional<Instant> getStartTimestamp() {
    return Optional.ofNullable(startTimestamp);
  }

  @JsonSerialize(using = OptionalInstantIsoSerializer.class)
  public Optional<Instant> getEndTimestamp() {
    return Optional.ofNullable(endTimestamp);
  }

  @Override
  public String toString() {
    return "OfflineRetransmissionRequest{"
        + "sourceTopic='"
        + sourceTopic
        + '\''
        + ", sourceViewPath='"
        + sourceViewPath
        + '\''
        + ", targetTopic='"
        + targetTopic
        + '\''
        + ", startTimestamp="
        + startTimestamp
        + ", endTimestamp="
        + endTimestamp
        + '}';
  }
}
