package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import com.google.common.base.Throwables;
import pl.allegro.tech.hermes.consumers.consumer.trace.TraceIdAppender;

import javax.jms.JMSException;
import javax.jms.Message;

import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_ID;

public class JmsTraceIdAppender implements TraceIdAppender<Message> {

    @Override
    public Message appendTraceId(Message target, pl.allegro.tech.hermes.consumers.consumer.Message message) {

        try {
            target.setStringProperty(TRACE_ID.getCamelCaseName(), message.getTraceId());
            return target;
        } catch(JMSException e) {
            Throwables.propagate(e);
            return null;
        }
    }
}
