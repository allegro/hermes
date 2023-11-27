package pl.allegro.tech.hermes.integrationtests.helpers;

import pl.allegro.tech.hermes.integrationtests.metadata.TraceContext;

import java.util.Map;

public class TraceHeaders {
    public static Map<String, String> fromTraceContext(TraceContext traceContext) {
        return Map.of(
                "Trace-Id", traceContext.getTraceId(),
                "Span-Id", traceContext.getSpanId(),
                "Parent-Span-Id", traceContext.getParentSpanId(),
                "Trace-Sampled", traceContext.getTraceSampled(),
                "Trace-Reported", traceContext.getTraceReported()
        );
    }
}
