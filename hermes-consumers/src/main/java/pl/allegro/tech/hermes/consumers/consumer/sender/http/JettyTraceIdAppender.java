package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.api.Request;
import pl.allegro.tech.hermes.common.http.MessageMetadataHeaders;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.trace.TraceIdAppender;

public class JettyTraceIdAppender implements TraceIdAppender<Request> {

    @Override
    public Request appendTraceId(Request target, Message message) {

        return target.header(MessageMetadataHeaders.TRACE_ID.getName(), message.getTraceId());
    }
}
