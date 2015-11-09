package pl.allegro.tech.hermes.integration.helper;

import pl.allegro.tech.hermes.integration.metadata.TraceContext;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

public class ClientBuilderHelper {

    public static Invocation.Builder createRequestWithTraceHeaders(String uri, String topicName, TraceContext traceContext) {

        WebTarget client = ClientBuilder.newClient().target(uri).path("topics").path(topicName);
        return client.request()
                .header("Trace-Id", traceContext.getTraceId())
                .header("Span-Id", traceContext.getSpanId())
                .header("Parent-Span-Id", traceContext.getParentSpanId())
                .header("Trace-Sampled", traceContext.getTraceSampled())
                .header("Trace-Reported", traceContext.getTraceReported());
    }
}
