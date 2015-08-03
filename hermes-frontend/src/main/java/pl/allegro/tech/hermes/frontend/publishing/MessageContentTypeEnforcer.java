package pl.allegro.tech.hermes.frontend.publishing;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.avro.JsonToAvroMessageConverter;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.common.schema.MessageSchemaRepository;

import javax.inject.Inject;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.Topic.ContentType.AVRO;

public class MessageContentTypeEnforcer {
    private final JsonToAvroMessageConverter messageConverter = new JsonToAvroMessageConverter();
    private final MessageSchemaRepository<Schema> messageSchemaRepository;

    @Inject
    public MessageContentTypeEnforcer(MessageSchemaRepository<Schema> messageSchemaRepository) {
        this.messageSchemaRepository = messageSchemaRepository;
    }

    public Message enforce(String messageContentType, Message message, Topic topic) {
        if (APPLICATION_JSON.equalsIgnoreCase(messageContentType) && AVRO == topic.getContentType()) {
            return messageConverter.convert(message, messageSchemaRepository.getSchema(topic)); // FIXME will lose messages, make this resilient to schema repository failures
        }
        return message;
    }
}
