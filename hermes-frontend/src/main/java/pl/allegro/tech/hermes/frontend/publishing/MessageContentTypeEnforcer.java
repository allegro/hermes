package pl.allegro.tech.hermes.frontend.publishing;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.common.avro.AvroConversionException;
import pl.allegro.tech.common.avro.JsonAvroConverter;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.frontend.publishing.avro.JsonToAvroMessageConverter;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.inject.Inject;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;

public class MessageContentTypeEnforcer {

    private static final Logger logger = LoggerFactory.getLogger(MessageContentTypeEnforcer.class);
    private final JsonToAvroMessageConverter jsonToAvroMessageConverter;

    @Inject
    public MessageContentTypeEnforcer(SchemaRepository<Schema> schemaRepository) {
        this.jsonToAvroMessageConverter = new JsonToAvroMessageConverter(schemaRepository, new JsonAvroConverter());
    }

    public Message enforce(String messageContentType, Message message, Topic topic) {
        if (APPLICATION_JSON.equalsIgnoreCase(messageContentType) && AVRO == topic.getContentType()) {
            return jsonToAvroMessageConverter.convert(message, topic); // TODO make this resilient to schema repository failures
        } else if (topic.isJsonToAvroDryRunEnabled() && JSON == topic.getContentType()) {
            try {
                jsonToAvroMessageConverter.convert(message, topic);
            } catch (AvroConversionException exception) {
                logger.warn("Unsuccessful message conversion from JSON to AVRO on topic {} in dry run mode",
                        topic.getQualifiedName(), exception);
            }
        }
        return message;
    }
}
