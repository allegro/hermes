package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public class OfflineRetransmissionRequest {
    @NotEmpty
    private final String sourceTopic;
    @NotEmpty
    private final String targetTopic;
    @NotNull
    private final Instant startTimestamp;
    @NotNull
    private final Instant endTimestamp;

    @JsonCreator
    public OfflineRetransmissionRequest(
            @JsonProperty("sourceTopic") String sourceTopic,
            @JsonProperty("targetTopic") String targetTopic,
            @JsonProperty("startTimestamp") Instant startTimestamp,
            @JsonProperty("endTimestamp") Instant endTimestamp) {
        this.sourceTopic = sourceTopic;
        this.targetTopic = targetTopic;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }

    public String getSourceTopic() {
        return sourceTopic;
    }

    public String getTargetTopic() {
        return targetTopic;
    }

    public Instant getStartTimestamp() {
        return startTimestamp;
    }

    public Instant getEndTimestamp() {
        return endTimestamp;
    }

    @Override
    public String toString() {
        return "OfflineRetransmissionRequest{" +
                "sourceTopic='" + sourceTopic + '\'' +
                ", targetTopic='" + targetTopic + '\'' +
                ", startTimestamp=" + startTimestamp +
                ", endTimestamp=" + endTimestamp +
                '}';
    }
}
