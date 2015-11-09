package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class TraceInfo {

    private final String traceId;

    private final String spanId;

    private final String parentSpanId;

    private final String traceSampled;

    private final String traceReported;

    @JsonCreator
    public TraceInfo(@JsonProperty("traceId") String traceId,
                     @JsonProperty("spanId") String spanId,
                     @JsonProperty("parentSpanId") String parentSpanId,
                     @JsonProperty("traceSampled") String traceSampled,
                     @JsonProperty("traceReported") String traceReported) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.traceSampled = traceSampled;
        this.traceReported = traceReported;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TraceInfo traceInfo = (TraceInfo) o;

        if (traceId != null ? !traceId.equals(traceInfo.traceId) : traceInfo.traceId != null) return false;
        if (spanId != null ? !spanId.equals(traceInfo.spanId) : traceInfo.spanId != null) return false;
        if (parentSpanId != null ? !parentSpanId.equals(traceInfo.parentSpanId) : traceInfo.parentSpanId != null)
            return false;
        if (traceSampled != null ? !traceSampled.equals(traceInfo.traceSampled) : traceInfo.traceSampled != null)
            return false;
        return !(traceReported != null ? !traceReported.equals(traceInfo.traceReported) : traceInfo.traceReported != null);

    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId, spanId, parentSpanId, traceSampled, traceReported);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String traceId;

        private String spanId;

        private String parentSpanId;

        private String traceSampled;

        private String traceReported;

        public Builder withTraceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder withSpanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        public Builder withParentSpanId(String parentSpanId) {
            this.parentSpanId = parentSpanId;
            return this;
        }

        public Builder withTraceSampled(String traceSampled) {
            this.traceSampled = traceSampled;
            return this;
        }

        public Builder withTraceReported(String traceReported) {
            this.traceReported = traceReported;
            return this;
        }

        public TraceInfo build() {
            return new TraceInfo(traceId, spanId, parentSpanId, traceSampled, traceReported);
        }
    }
}
