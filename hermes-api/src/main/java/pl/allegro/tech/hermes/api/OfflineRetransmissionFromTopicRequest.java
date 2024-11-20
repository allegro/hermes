package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import pl.allegro.tech.hermes.api.constraints.TimeRangeForTopicRetransmission;
import pl.allegro.tech.hermes.api.jackson.InstantIsoSerializer;

@TimeRangeForTopicRetransmission
public final class OfflineRetransmissionFromTopicRequest extends OfflineRetransmissionRequest {

  @NotNull private final String sourceTopic;
  @NotNull private final Instant startTimestamp;
  @NotNull private final Instant endTimestamp;

  @JsonCreator
  public OfflineRetransmissionFromTopicRequest(
      @JsonProperty("sourceTopic") String sourceTopic,
      @JsonProperty("targetTopic") String targetTopic,
      @JsonProperty("startTimestamp") String startTimestamp,
      @JsonProperty("endTimestamp") String endTimestamp) {
    super(RetransmissionType.TOPIC, targetTopic);
    this.sourceTopic = sourceTopic;
    this.startTimestamp = initializeTimestamp(startTimestamp);
    this.endTimestamp = initializeTimestamp(endTimestamp);
  }

  public String getSourceTopic() {
    return sourceTopic;
  }

  @JsonSerialize(using = InstantIsoSerializer.class)
  public Instant getStartTimestamp() {
    return startTimestamp;
  }

  @JsonSerialize(using = InstantIsoSerializer.class)
  public Instant getEndTimestamp() {
    return endTimestamp;
  }

  @Override
  public String toString() {
    return "OfflineRetransmissionFromTopicRequest{"
        + "sourceTopic='"
        + sourceTopic
        + '\''
        + ", targetTopic='"
        + getTargetTopic()
        + '\''
        + ", startTimestamp="
        + startTimestamp
        + ", endTimestamp="
        + endTimestamp
        + '}';
  }
}
