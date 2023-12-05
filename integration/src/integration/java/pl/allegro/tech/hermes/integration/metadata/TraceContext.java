package pl.allegro.tech.hermes.integration.metadata;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.UUID;

public class TraceContext {

    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final String traceSampled;
    private final String traceReported;

    public TraceContext(String traceId, String spanId, String parentSpanId, String traceSampled, String traceReported) {
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

    public Map<String, String> asMap() {

        return ImmutableMap.<String, String>builder()
                .put("trace-id", traceId)
                .put("span-id", spanId)
                .put("parent-span-id", parentSpanId)
                .put("trace-sampled", traceSampled)
                .put("trace-reported", traceReported)
                .build();
    }

    public static TraceContext random() {

        return new Builder()
                .withTraceId(UUID.randomUUID().toString())
                .withSpanId(UUID.randomUUID().toString())
                .withParentSpanId(UUID.randomUUID().toString())
                .withTraceSampled("1")
                .withTraceReported("0")
                .build();
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

        public TraceContext build() {
            return new TraceContext(traceId, spanId, parentSpanId, traceSampled, traceReported);
        }
    }
}
