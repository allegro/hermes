package pl.allegro.tech.hermes.common.message.wrapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class MessageMetadata {

    private final long timestamp;
    private final String id;
    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final String traceSampled;
    private final String traceReported;

    @JsonCreator
    public MessageMetadata(@JsonProperty("timestamp") long timestamp,
                           @JsonProperty("id") String id,
                           @JsonProperty("traceId") String traceId,
                           @JsonProperty("spanId") String spanId,
                           @JsonProperty("parentSpanId") String parentSpanId,
                           @JsonProperty("traceSampled") String traceSampled,
                           @JsonProperty("traceReported") String traceReported) {
        this.id = id;
        this.timestamp = timestamp;
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.traceSampled = traceSampled;
        this.traceReported = traceReported;
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

    public String getSpanId() {
        return spanId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public String getTraceSampled() {
        return traceSampled;
    }

    public String getTraceReported() {
        return traceReported;
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
