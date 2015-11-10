package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import java.util.Map;

public class JmsMetadataAppender implements MetadataAppender<Message> {

    @Override
    public Message append(Message target, pl.allegro.tech.hermes.consumers.consumer.Message message) {

        try {
            for(Map.Entry<String, String> entry : message.getExternalMetadata().entrySet()) {
                target.setStringProperty(normalize(entry.getKey()), entry.getValue());
            }
            return target;
        } catch(JMSException e) {
            throw new JMSRuntimeException(e.getMessage(), e.getErrorCode(), e);
        }
    }

    private static String normalize(String key) {
        return key.replaceAll("-", "");
    }
}
