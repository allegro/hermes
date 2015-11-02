package pl.allegro.tech.hermes.common.message.wrapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class MessageMetadata {

    private final long timestamp;
    private final String id;
    private final String traceId;

    @JsonCreator
    public MessageMetadata(@JsonProperty("timestamp") long timestamp,
                           @JsonProperty("id") String id,
                           @JsonProperty("traceId") String traceId) {
        this.id = id;
        this.timestamp = timestamp;
        this.traceId = traceId;
    }

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final MessageMetadata other = (MessageMetadata) obj;
        return Objects.equals(this.id, other.id)
                && Objects.equals(this.timestamp, other.timestamp);
    }
}
