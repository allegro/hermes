package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import com.google.common.base.Throwables;
import pl.allegro.tech.hermes.consumers.consumer.trace.TraceAppender;

import javax.jms.JMSException;
import javax.jms.Message;

import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.PARENT_SPAN_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SPAN_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_REPORTED;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_SAMPLED;

public class JmsTraceAppender implements TraceAppender<Message> {

    @Override
    public Message appendTraceInfo(Message target, pl.allegro.tech.hermes.consumers.consumer.Message message) {

        try {
            target.setStringProperty(TRACE_ID.getCamelCaseName(), message.getMessageTrace().getTraceId());
            target.setStringProperty(SPAN_ID.getCamelCaseName(), message.getMessageTrace().getSpanId());
            target.setStringProperty(PARENT_SPAN_ID.getCamelCaseName(), message.getMessageTrace().getParentSpanId());
            target.setStringProperty(TRACE_SAMPLED.getCamelCaseName(), message.getMessageTrace().getTraceSampled());
            target.setStringProperty(TRACE_REPORTED.getCamelCaseName(), message.getMessageTrace().getTraceReported());
            return target;
        } catch(JMSException e) {
            Throwables.propagate(e);
            return null;
        }
    }
}
