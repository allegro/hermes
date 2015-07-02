package pl.allegro.tech.hermes.frontend.publishing;


import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.avro.JsonToAvroMessageConverter;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.Topic.ContentType.AVRO;

public class MessageContentTypeEnforcer {
    private final JsonToAvroMessageConverter messageConverter = new JsonToAvroMessageConverter();

    public Message enforce(String messageContentType, Message message, Topic topic) {
        if (APPLICATION_JSON.equalsIgnoreCase(messageContentType) && AVRO == topic.getContentType()) {
            return messageConverter.convert(message, topic.getMessageSchema());
        }
        return message;
    }
}
