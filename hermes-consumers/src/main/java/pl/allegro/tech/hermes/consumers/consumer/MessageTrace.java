package pl.allegro.tech.hermes.consumers.consumer;

import java.util.Objects;

public class MessageTrace {

    private final String traceId;

    private final String spanId;

    private final String parentSpanId;

    private final String traceSampled;

    private final String traceReported;

    public MessageTrace(String traceId, String spanId, String parentSpanId, String traceSampled, String traceReported) {
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

        MessageTrace that = (MessageTrace) o;

        if (traceId != null ? !traceId.equals(that.traceId) : that.traceId != null) return false;
        if (spanId != null ? !spanId.equals(that.spanId) : that.spanId != null) return false;
        if (parentSpanId != null ? !parentSpanId.equals(that.parentSpanId) : that.parentSpanId != null) return false;
        if (traceSampled != null ? !traceSampled.equals(that.traceSampled) : that.traceSampled != null) return false;
        return !(traceReported != null ? !traceReported.equals(that.traceReported) : that.traceReported != null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId, spanId, parentSpanId, traceSampled, traceReported);
    }
}
