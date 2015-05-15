package pl.allegro.tech.hermes.frontend.validator;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicCallback;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static pl.allegro.tech.hermes.common.metric.Metrics.Timer.PRODUCER_VALIDATION_LATENCY;

public class MessageValidator implements TopicCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageValidator.class);

    private final Map<TopicName, JsonSchema> topicsWithValidation;
    private final ObjectMapper objectMapper;
    private final HermesMetrics hermesMetrics;
    private final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();

    @Inject
    public MessageValidator(ObjectMapper objectMapper, HermesMetrics hermesMetrics) {
        this.objectMapper = objectMapper;
        this.hermesMetrics = hermesMetrics;
        topicsWithValidation = new ConcurrentHashMap<>();
    }

    @Override
    public void onTopicCreated(Topic topic) {
        if (topic.isValidationEnabled()) {
            addValidation(topic);
        }
    }

    @Override
    public void onTopicRemoved(Topic topic) {
        logIfSchemaRemoved(topicsWithValidation.remove(topic.getName()), topic.getQualifiedName());
    }

    @Override
    public void onTopicChanged(Topic topic) {
        if (topic.isValidationEnabled()) {
            addValidation(topic);
        } else {
            logIfSchemaRemoved(topicsWithValidation.remove(topic.getName()), topic.getQualifiedName());
        }
    }

    public void check(TopicName topicName, byte[] message) {
        List<String> errors = validate(topicName, message);
        if (!errors.isEmpty()) {
            throw new InvalidMessageException("Message incompatible with JSON schema", errors);
        }
    }

    private List<String> validate(TopicName topicName, byte[] message) {
        JsonSchema jsonSchema = topicsWithValidation.get(topicName);

        if (jsonSchema == null) {
            return ImmutableList.of();
        }

        Timer.Context validationTimer = hermesMetrics.timer(PRODUCER_VALIDATION_LATENCY).time();
        Timer.Context validationTimerPerTopic = hermesMetrics.timer(PRODUCER_VALIDATION_LATENCY, topicName).time();

        List<String> errors = validate(jsonSchema, message);

        validationTimer.close();
        validationTimerPerTopic.close();

        return errors;
    }

    private void addValidation(Topic topic) {
        try {
            topicsWithValidation.put(topic.getName(), createJsonSchema(topic));
            LOGGER.info("Enabled validation for topic {}", topic.getQualifiedName());
        } catch (Exception e) {
            LOGGER.error("Error while creating json schema for topic: " + topic.getName(), e);
        }
    }

    private List<String> validate(JsonSchema jsonSchema, byte[] message) {
        List<String> errors = new ArrayList<>();

        try {
            JsonNode messageNode = objectMapper.readTree(message);

            ProcessingReport report = jsonSchema.validateUnchecked(messageNode);

            for (Iterator<ProcessingMessage> iterator = report.iterator(); iterator.hasNext();) {
                errors.add(iterator.next().getMessage());
            }

        } catch (IOException e) {
            LOGGER.warn("Error while deserializing message: " + new String(message), e);
            errors.add("Problem with message deserialization. Is this correct JSON format?");
        }

        return errors;
    }

    private JsonSchema createJsonSchema(Topic topic) throws IOException, ProcessingException {
        if (isNullOrEmpty(topic.getMessageSchema())) {
            throw new IllegalArgumentException("Message schema is empty for topic: " + topic.getQualifiedName());
        }

        JsonNode schemaNode = objectMapper.readTree(topic.getMessageSchema());

        return jsonSchemaFactory.getJsonSchema(schemaNode);
    }

    private void logIfSchemaRemoved(JsonSchema previousSchema, String topicQualifiedName) {
        if (previousSchema != null) {
            LOGGER.info("Disabled validation for topic: {}", topicQualifiedName);
        }
    }
}
