package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest.RetransmissionType;
import pl.allegro.tech.hermes.api.jackson.InstantIsoSerializer;
import pl.allegro.tech.hermes.api.jackson.OptionalInstantIsoSerializer;

public class OfflineRetransmissionTask {
  private final RetransmissionType type;
  private final String taskId;
  @Nullable private final String sourceViewPath;
  @Nullable private final String sourceTopic;
  private final String targetTopic;
  @Nullable private final Instant startTimestamp;
  @Nullable private final Instant endTimestamp;
  private final Instant createdAt;

  public OfflineRetransmissionTask(
      RetransmissionType type,
      String taskId,
      @Nullable String sourceViewPath,
      @Nullable String sourceTopic,
      String targetTopic,
      @Nullable Instant startTimestamp,
      @Nullable Instant endTimestamp,
      Instant createdAt) {
    this.taskId = taskId;
    this.sourceViewPath = sourceViewPath;
    this.sourceTopic = sourceTopic;
    this.targetTopic = targetTopic;
    this.startTimestamp = startTimestamp;
    this.endTimestamp = endTimestamp;
    this.createdAt = createdAt;
    this.type = type;
  }

  @JsonCreator
  public OfflineRetransmissionTask(
      @JsonProperty("type") @Nullable RetransmissionType type,
      @JsonProperty("taskId") String taskId,
      @JsonProperty("sourceViewPath") @Nullable String sourceViewPath,
      @JsonProperty("sourceTopic") @Nullable String sourceTopic,
      @JsonProperty("targetTopic") String targetTopic,
      @JsonProperty("startTimestamp") @Nullable String startTimestamp,
      @JsonProperty("endTimestamp") @Nullable String endTimestamp,
      @JsonProperty("createdAt") String createdAt) {
    /*
    TODO: Needed for backward compatibility when reading existing retransmissions from zookeeper, remove this once the
    new version is rolled out to all environments.
     */
    this.type = Objects.requireNonNullElse(type, RetransmissionType.TOPIC);
    this.taskId = taskId;
    this.sourceViewPath = sourceViewPath;
    this.sourceTopic = sourceTopic;
    this.targetTopic = targetTopic;
    this.startTimestamp = OfflineRetransmissionFromTopicRequest.initializeTimestamp(startTimestamp);
    this.endTimestamp = OfflineRetransmissionFromTopicRequest.initializeTimestamp(endTimestamp);
    this.createdAt = OfflineRetransmissionFromTopicRequest.initializeTimestamp(createdAt);
  }

  public String getTaskId() {
    return taskId;
  }

  public Optional<String> getSourceTopic() {
    return Optional.ofNullable(sourceTopic);
  }

  public Optional<String> getSourceViewPath() {
    return Optional.ofNullable(sourceViewPath);
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

  @JsonSerialize(using = InstantIsoSerializer.class)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public RetransmissionType getType() {
    return type;
  }

  @Override
  public String toString() {
    return "OfflineRetransmissionTask{"
        + "type="
        + type
        + ", taskId='"
        + taskId
        + '\''
        + ", sourceViewPath='"
        + sourceViewPath
        + '\''
        + ", sourceTopic='"
        + sourceTopic
        + '\''
        + ", targetTopic='"
        + targetTopic
        + '\''
        + ", startTimestamp="
        + startTimestamp
        + ", endTimestamp="
        + endTimestamp
        + ", createdAt="
        + createdAt
        + '}';
  }
}
