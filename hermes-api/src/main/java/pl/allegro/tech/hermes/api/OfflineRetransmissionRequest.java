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
import java.time.format.DateTimeParseException;
import java.util.List;

public class OfflineRetransmissionRequest {

    private static final List<DateTimeFormatter> formatters = List.of(
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").withZone(ZoneId.of("UTC"))
    );

    @NotEmpty
    private final String sourceTopic;
    @NotEmpty
    private final String targetTopic;
    @NotNull
    private Instant startTimestamp;
    @NotNull
    private Instant endTimestamp;

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

        for (DateTimeFormatter formatter : formatters) {
            try {
                this.startTimestamp = formatter.parse(startTimestamp, Instant::from);
                this.endTimestamp = formatter.parse(endTimestamp, Instant::from);
                break;
            } catch (DateTimeParseException ignored) {}
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
