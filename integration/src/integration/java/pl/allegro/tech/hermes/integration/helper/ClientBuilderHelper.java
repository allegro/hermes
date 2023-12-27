package pl.allegro.tech.hermes.integration.helper;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import pl.allegro.tech.hermes.integration.metadata.TraceContext;

public class ClientBuilderHelper {

    public static Invocation.Builder createRequestWithTraceHeaders(String uri, String topicName, TraceContext traceContext) {

        WebTarget client = ClientBuilder.newClient().target(uri).path("topics").path(topicName);
        return client.request()
                .header("trace-id", traceContext.getTraceId())
                .header("span-id", traceContext.getSpanId())
                .header("parent-span-id", traceContext.getParentSpanId())
                .header("trace-sampled", traceContext.getTraceSampled())
                .header("trace-reported", traceContext.getTraceReported());
    }
}
