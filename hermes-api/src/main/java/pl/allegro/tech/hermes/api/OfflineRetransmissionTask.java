package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import pl.allegro.tech.hermes.api.jackson.InstantIsoSerializer;

import java.time.Instant;

public class OfflineRetransmissionTask {
    private final String taskId;
    private final OfflineRetransmissionRequest request;
    private final Instant createdAt;

    @JsonCreator
    public OfflineRetransmissionTask(
            @JsonProperty("taskId") String taskId,
            @JsonProperty("sourceTopic") String sourceTopic,
            @JsonProperty("targetTopic") String targetTopic,
            @JsonProperty("startTimestamp") Instant startTimestamp,
            @JsonProperty("endTimestamp") Instant endTimestamp,
            @JsonProperty("createdAt") Instant createdAt) {
        this(taskId, new OfflineRetransmissionRequest(sourceTopic, targetTopic, startTimestamp, endTimestamp), createdAt);
    }

    public OfflineRetransmissionTask(String taskId, OfflineRetransmissionRequest request, Instant createdAt) {
        this.taskId = taskId;
        this.request = request;
        this.createdAt = createdAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getSourceTopic() {
        return request.getSourceTopic();
    }

    public String getTargetTopic() {
        return request.getTargetTopic();
    }

    @JsonSerialize(using = InstantIsoSerializer.class)
    public Instant getStartTimestamp() {
        return request.getStartTimestamp();
    }

    @JsonSerialize(using = InstantIsoSerializer.class)
    public Instant getEndTimestamp() {
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
        return "OfflineRetransmissionTask{"
                + "taskId='" + taskId + '\''
                + ", request=" + request
                + '}';
    }
}
