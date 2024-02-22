package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.jackson.InstantIsoSerializer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class OfflineRetransmissionRequest {

    private static final List<DateTimeFormatter> formatters = List.of(
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC")),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").withZone(ZoneId.of("UTC"))
    );
    private static final Logger logger = LoggerFactory.getLogger(OfflineRetransmissionRequest.class);

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
