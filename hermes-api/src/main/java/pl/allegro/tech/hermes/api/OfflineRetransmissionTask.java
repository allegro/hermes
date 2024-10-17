package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.Optional;
import javax.annotation.Nullable;
import pl.allegro.tech.hermes.api.jackson.InstantIsoSerializer;
import pl.allegro.tech.hermes.api.jackson.OptionalInstantIsoSerializer;

public class OfflineRetransmissionTask {
  private final String taskId;
  private final OfflineRetransmissionRequest request;
  private final Instant createdAt;

  @JsonCreator
  public OfflineRetransmissionTask(
      @JsonProperty("taskId") String taskId,
      @JsonProperty("sourceViewPath") String sourceViewPath,
      @JsonProperty("sourceTopic") String sourceTopic,
      @JsonProperty("targetTopic") String targetTopic,
      @JsonProperty("startTimestamp") Instant startTimestamp,
      @JsonProperty("endTimestamp") Instant endTimestamp,
      @JsonProperty("createdAt") Instant createdAt) {
    this(
        taskId,
        new OfflineRetransmissionRequest(
            sourceViewPath,
            sourceTopic,
            targetTopic,
            parseTimestamp(startTimestamp),
            parseTimestamp(endTimestamp)),
        createdAt);
  }

  public OfflineRetransmissionTask(
      String taskId, OfflineRetransmissionRequest request, Instant createdAt) {
    this.taskId = taskId;
    this.request = request;
    this.createdAt = createdAt;
  }

  public String getTaskId() {
    return taskId;
  }

  public Optional<String> getSourceTopic() {
    return request.getSourceTopic();
  }

  public Optional<String> getSourceViewPath() {
    return request.getSourceViewPath();
  }

  public String getTargetTopic() {
    return request.getTargetTopic();
  }

  @JsonSerialize(using = OptionalInstantIsoSerializer.class)
  public Optional<Instant> getStartTimestamp() {
    return request.getStartTimestamp();
  }

  @JsonSerialize(using = OptionalInstantIsoSerializer.class)
  public Optional<Instant> getEndTimestamp() {
    return request.getEndTimestamp();
  }

  @JsonSerialize(using = InstantIsoSerializer.class)
  public Instant getCreatedAt() {
    return createdAt;
  }

  @JsonIgnore
  public OfflineRetransmissionRequest getRequest() {
    return request;
  }

  @Override
  public String toString() {
    return "OfflineRetransmissionTask{" + "taskId='" + taskId + '\'' + ", request=" + request + '}';
  }

  private static String parseTimestamp(@Nullable Instant startTimestamp) {
    if (startTimestamp == null) {
      return null;
    }
    return startTimestamp.toString();
  }
}
