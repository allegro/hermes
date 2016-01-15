package pl.allegro.tech.hermes.frontend.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class JsonTopicMessageValidator implements TopicMessageValidator {

    private static final Logger logger = LoggerFactory.getLogger(JsonTopicMessageValidator.class);
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private final SchemaRepository<JsonSchema> schemaRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public JsonTopicMessageValidator(SchemaRepository<JsonSchema> schemaRepository, ObjectMapper objectMapper) {
        this.schemaRepository = schemaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void check(byte[] message, Topic topic) {
        if (ContentType.JSON != topic.getContentType() || !topic.isValidationEnabled()) {
            return;
        }

        List<String> errors = validate(schemaRepository.getSchema(topic), message);

        if (!errors.isEmpty()) {
            if (topic.isValidationDryRunEnabled()) {
                logger.info("Message incompatible with JSON schema for topic {}, errors: {}, message body: {}",
                        topic.getQualifiedName(), Joiner.on(";").join(errors), new String(message, UTF_8));
            } else {
                throw new InvalidMessageException("Message incompatible with JSON schema", errors);
            }
        }
    }

    private List<String> validate(JsonSchema jsonSchema, byte[] message) {
        List<String> errors = new ArrayList<>();

        try {
            JsonNode messageNode = objectMapper.readTree(message);

            ProcessingReport report = jsonSchema.validateUnchecked(messageNode);

            report.forEach(processingMessage -> errors.add(processingMessage.getMessage()));

        } catch (IOException e) {
            logger.warn("Error while deserializing message: " + new String(message), e);
            errors.add("Problem with message deserialization. Is this correct JSON format?");
        }

        return errors;
    }

}
