package pl.allegro.tech.hermes.frontend.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.schema.MessageSchemaRepository;
import pl.allegro.tech.hermes.frontend.schema.MessageSchemaSourceRepository;

import javax.inject.Inject;
import java.io.IOException;

import static com.google.common.base.Strings.isNullOrEmpty;

public class TopicMessageValidatorFactory {
    private final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();
    private final MessageSchemaSourceRepository schemaRepository;
    private final ObjectMapper objectMapper;

    private final AvroTopicMessageValidator avroTopicMessageValidator;

    @Inject
    public TopicMessageValidatorFactory(MessageSchemaSourceRepository schemaRepository, MessageSchemaRepository<Schema> avroMessageSchemaRepository, ObjectMapper objectMapper) {
        this.schemaRepository = schemaRepository;
        this.objectMapper = objectMapper;
        this.avroTopicMessageValidator = new AvroTopicMessageValidator(avroMessageSchemaRepository);
    }

    public TopicMessageValidator create(Topic topic) throws IOException, ProcessingException {
        if (isNullOrEmpty(schemaRepository.getSchemaSource(topic))) {
            throw new IllegalArgumentException("Message schema is empty for topic: " + topic.getQualifiedName());
        }

        switch (topic.getContentType()) {
            case JSON:
                return createJsonTopicMessageValidator(topic);
            case AVRO:
                return avroTopicMessageValidator;
            default:
                throw new IllegalStateException("Unsupported content type " + topic.getContentType().name());
        }
    }

    private TopicMessageValidator createJsonTopicMessageValidator(Topic topic) throws IOException, ProcessingException {
        return new JsonTopicMessageValidator(
            jsonSchemaFactory.getJsonSchema(objectMapper.readTree(schemaRepository.getSchemaSource(topic))),
            objectMapper);
    }
}
