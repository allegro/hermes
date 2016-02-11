package pl.allegro.tech.hermes.frontend.publishing;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.frontend.publishing.avro.JsonToAvroMessageConverter;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import tech.allegro.schema.json2avro.converter.AvroConversionException;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import javax.inject.Inject;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;

public class MessageContentTypeEnforcer {

    private static final Logger logger = LoggerFactory.getLogger(MessageContentTypeEnforcer.class);

    private final JsonToAvroMessageConverter jsonToAvroMessageConverter;

    private static final String APPLICATION_JSON_WITH_DELIM = APPLICATION_JSON + ";";

    @Inject
    public MessageContentTypeEnforcer(SchemaRepository<Schema> schemaRepository) {
        this.jsonToAvroMessageConverter = new JsonToAvroMessageConverter(schemaRepository, new JsonAvroConverter());
    }

    public Message enforce(String messageContentType, Message message, Topic topic) {
        if (topic.getContentType() == AVRO && isJSON(messageContentType)) {
            return jsonToAvroMessageConverter.convert(message, topic);
        } else if (topic.isJsonToAvroDryRunEnabled() && JSON == topic.getContentType()) {
            performDryRunValidation(message, topic);
        }
        return message;
    }

    private boolean isJSON(String contentType) {
        return contentType != null && (contentType.length() > APPLICATION_JSON.length() ?
                contentType.startsWith(APPLICATION_JSON_WITH_DELIM) : contentType.equals(APPLICATION_JSON));
    }

    private void performDryRunValidation(Message message, Topic topic) {
        try {
            jsonToAvroMessageConverter.convert(message, topic);
        } catch (AvroConversionException exception) {
            logger.warn("Unsuccessful message conversion from JSON to AVRO on topic {} in dry run mode",
                    topic.getQualifiedName(), exception);
        }
    }
}
