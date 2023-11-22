package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import pl.allegro.tech.hermes.api.jackson.InstantIsoSerializer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class OfflineRetransmissionRequest {
    @NotEmpty
    private final String sourceTopic;
    @NotEmpty
    private final String targetTopic;
    @NotNull
    private Instant startTimestamp;
    @NotNull
    private Instant endTimestamp;

    private final static String ALTERNATE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm'Z'";

    @JsonCreator
    public OfflineRetransmissionRequest(
            @JsonProperty("sourceTopic") String sourceTopic,
            @JsonProperty("targetTopic") String targetTopic,
            @JsonProperty("startTimestamp") String startTimestamp,
            @JsonProperty("endTimestamp") String endTimestamp) {
        this.sourceTopic = sourceTopic;
        this.targetTopic = targetTopic;
        initializeTimestamps(startTimestamp, endTimestamp);
    }

    private void initializeTimestamps(String startTimestamp, String endTimestamp) {
        if (startTimestamp == null || endTimestamp == null) {
            this.startTimestamp = null;
            this.endTimestamp = null;
            return;
        }
        try {
            this.startTimestamp = Instant.parse(startTimestamp);
            this.endTimestamp = Instant.parse(endTimestamp);
        } catch (Exception e) {
            this.startTimestamp = Instant.from(DateTimeFormatter.ofPattern(ALTERNATE_DATE_FORMAT)
                    .withZone(ZoneId.of("UTC"))
                    .parse(startTimestamp));
            this.endTimestamp = Instant.from(DateTimeFormatter.ofPattern(ALTERNATE_DATE_FORMAT)
                    .withZone(ZoneId.of("UTC"))
                    .parse(endTimestamp));
        }
    }

    public String getSourceTopic() {
        return sourceTopic;
    }

    public String getTargetTopic() {
        return targetTopic;
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
        return "OfflineRetransmissionRequest{"
                + "sourceTopic='" + sourceTopic + '\''
                + ", targetTopic='" + targetTopic + '\''
                + ", startTimestamp=" + startTimestamp
                + ", endTimestamp=" + endTimestamp
                + '}';
    }
}
