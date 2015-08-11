package pl.allegro.tech.hermes.frontend.publishing;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.frontend.publishing.avro.JsonToAvroMessageConverter;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.inject.Inject;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.Topic.ContentType.AVRO;

public class MessageContentTypeEnforcer {
    private final JsonToAvroMessageConverter jsonToAvroMessageConverter;

    @Inject
    public MessageContentTypeEnforcer(SchemaRepository<Schema> schemaRepository) {
        this.jsonToAvroMessageConverter = new JsonToAvroMessageConverter(schemaRepository);
    }

    public Message enforce(String messageContentType, Message message, Topic topic) {
        if (APPLICATION_JSON.equalsIgnoreCase(messageContentType) && AVRO == topic.getContentType()) {
            return jsonToAvroMessageConverter.convert(message, topic); // TODO make this resilient to schema repository failures
        }
        return message;
    }
}
