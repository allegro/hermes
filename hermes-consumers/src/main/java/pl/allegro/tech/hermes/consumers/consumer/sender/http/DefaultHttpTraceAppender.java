package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.api.Request;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.trace.TraceAppender;

import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.PARENT_SPAN_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SPAN_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_REPORTED;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_SAMPLED;

public class DefaultHttpTraceAppender implements TraceAppender<Request> {

    @Override
    public Request appendTraceInfo(Request target, Message message) {

        return target.header(TRACE_ID.getName(), message.getMessageTrace().getTraceId())
                .header(SPAN_ID.getName(), message.getMessageTrace().getSpanId())
                .header(PARENT_SPAN_ID.getName(), message.getMessageTrace().getParentSpanId())
                .header(TRACE_SAMPLED.getName(), message.getMessageTrace().getTraceSampled())
                .header(TRACE_REPORTED.getName(), message.getMessageTrace().getTraceReported());
    }
}
