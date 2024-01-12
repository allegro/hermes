package pl.allegro.tech.hermes.integrationtests.helpers;

import pl.allegro.tech.hermes.integrationtests.metadata.TraceContext;

import java.util.Map;

public class TraceHeaders {
    public static Map<String, String> fromTraceContext(TraceContext traceContext) {
        return Map.of(
                "Trace-Id", traceContext.traceId(),
                "Span-Id", traceContext.spanId(),
                "Parent-Span-Id", traceContext.parentSpanId(),
                "Trace-Sampled", traceContext.traceSampled(),
                "Trace-Reported", traceContext.traceReported()
        );
    }
}
