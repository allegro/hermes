package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import java.util.Map;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;

public class JmsMetadataAppender implements MetadataAppender<Message> {

    private static String normalize(String key) {
        return key.replaceAll("-", "");
    }

    @Override
    public Message append(Message target, pl.allegro.tech.hermes.consumers.consumer.Message message) {

        try {
            for (Map.Entry<String, String> entry : message.getExternalMetadata().entrySet()) {
                target.setStringProperty(normalize(entry.getKey()), entry.getValue());
            }
            return target;
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage(), e.getErrorCode(), e);
        }
    }
}
