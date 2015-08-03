package pl.allegro.tech.hermes.frontend.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.schema.MessageSchemaRepository;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonTopicMessageValidator implements TopicMessageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonTopicMessageValidator.class);
    private final MessageSchemaRepository<JsonSchema> messageSchemaRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public JsonTopicMessageValidator(MessageSchemaRepository<JsonSchema> messageSchemaRepository, ObjectMapper objectMapper) {
        this.messageSchemaRepository = messageSchemaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void check(byte[] message, Topic topic) {
        if (!topic.getContentType().equals(Topic.ContentType.JSON) || !topic.isValidationEnabled()) {
            return;
        }

        List<String> errors = validate(messageSchemaRepository.getSchema(topic), message);

        if (!errors.isEmpty()) {
            throw new InvalidMessageException("Message incompatible with JSON schema", errors);
        }
    }

    private List<String> validate(JsonSchema jsonSchema, byte[] message) {
        List<String> errors = new ArrayList<>();

        try {
            JsonNode messageNode = objectMapper.readTree(message);

            ProcessingReport report = jsonSchema.validateUnchecked(messageNode);

            report.forEach(processingMessage -> errors.add(processingMessage.getMessage()));

        } catch (IOException e) {
            LOGGER.warn("Error while deserializing message: " + new String(message), e);
            errors.add("Problem with message deserialization. Is this correct JSON format?");
        }

        return errors;
    }

}
