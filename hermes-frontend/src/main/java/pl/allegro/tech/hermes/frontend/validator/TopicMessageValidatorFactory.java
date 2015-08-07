package pl.allegro.tech.hermes.frontend.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;

import javax.inject.Inject;
import java.io.IOException;

import static com.google.common.base.Strings.isNullOrEmpty;

public class TopicMessageValidatorFactory {
    private final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();
    private final ObjectMapper objectMapper;

    @Inject
    public TopicMessageValidatorFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TopicMessageValidator create(Topic topic) throws IOException, ProcessingException {
        if (isNullOrEmpty(topic.getMessageSchema())) {
            throw new IllegalArgumentException("Message schema is empty for topic: " + topic.getQualifiedName());
        }

        switch (topic.getContentType()) {
            case JSON:
                return createJsonTopicMessageValidator(topic);
            case AVRO:
                return createAvroTopicMessageValidator(topic);
            default:
                throw new IllegalStateException("Unsupported content type " + topic.getContentType().name());
        }
    }

    private TopicMessageValidator createAvroTopicMessageValidator(Topic topic) {
        return new AvroTopicMessageValidator(new Schema.Parser().parse(topic.getMessageSchema()));
    }

    private TopicMessageValidator createJsonTopicMessageValidator(Topic topic) throws IOException, ProcessingException {
        return new JsonTopicMessageValidator(
            jsonSchemaFactory.getJsonSchema(objectMapper.readTree(topic.getMessageSchema())),
            objectMapper);
    }
}
