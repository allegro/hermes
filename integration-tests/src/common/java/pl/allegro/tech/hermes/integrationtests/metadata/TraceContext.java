package pl.allegro.tech.hermes.integrationtests.metadata;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.UUID;

public record TraceContext(
    String traceId, String spanId, String parentSpanId, String traceSampled, String traceReported) {

  public Map<String, String> asMap() {

    return ImmutableMap.<String, String>builder()
        .put("Trace-Id", traceId)
        .put("Span-Id", spanId)
        .put("Parent-Span-Id", parentSpanId)
        .put("Trace-Sampled", traceSampled)
        .put("Trace-Reported", traceReported)
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
