package pl.allegro.tech.hermes.frontend.publishing.message;

import com.github.fge.jsonschema.main.JsonSchema;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.http.MessageMetadataHeaders;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion;
import pl.allegro.tech.hermes.frontend.metric.StartedTimersPair;
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage;
import pl.allegro.tech.hermes.frontend.publishing.handlers.AttachmentContent;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import tech.allegro.schema.json2avro.converter.AvroConversionException;

import javax.inject.Inject;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.of;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

public class MessageFactory {
    private static final Logger logger = LoggerFactory.getLogger(MessageFactory.class);

    private final MessageValidators validators;
    private final MessageContentTypeEnforcer enforcer;
    private final SchemaRepository schemaRepository;
    private final HeadersPropagator headersPropagator;
    private final MessageContentWrapper messageContentWrapper;
    private final Clock clock;

    @Inject
    public MessageFactory(MessageValidators validators,
                          MessageContentTypeEnforcer enforcer,
                          SchemaRepository schemaRepository,
                          HeadersPropagator headersPropagator,
                          MessageContentWrapper messageContentWrapper,
                          Clock clock) {
        this.validators = validators;
        this.enforcer = enforcer;
        this.messageContentWrapper = messageContentWrapper;
        this.schemaRepository = schemaRepository;
        this.headersPropagator = headersPropagator;
        this.clock = clock;
    }

    public Message create(HeaderMap headerMap, AttachmentContent attachment) {
        try (StartedTimersPair startedTimersPair = attachment.getTopicWithMetrics().startMessageCreationTimers()) {
            return create(
                    headerMap,
                    attachment.getTopic(),
                    attachment.getMessageId(),
                    attachment.getMessageContent());
        }
    }

    private Message create(HeaderMap headerMap, Topic topic, String messageId, byte[] messageContent) {
        long timestamp = clock.millis();
        switch (topic.getContentType()) {
            case JSON: {
                if (topic.isJsonToAvroDryRunEnabled()) {
                    try {
                        createAvroMessage(headerMap, topic, messageId, messageContent, timestamp);
                    } catch (AvroConversionException exception) {
                        logger.warn("Unsuccessful message conversion from JSON to AVRO on topic {} in dry run mode",
                                topic.getQualifiedName(), exception);
                    }
                }
                return createJsonMessage(headerMap, topic, messageId, messageContent, timestamp);
            }
            case AVRO:
                return createAvroMessage(headerMap, topic, messageId, messageContent, timestamp);
            default: throw new UnsupportedContentTypeException(topic);
        }
    }

    private AvroMessage createAvroMessage(HeaderMap headerMap, Topic topic, String messageId, byte[] messageContent, long timestamp) {
        CompiledSchema<Schema> schema = extractSchemaVersion(headerMap)
                .map(version -> schemaRepository.getAvroSchema(topic, version))
                .orElse(schemaRepository.getAvroSchema(topic));

        AvroMessage message = new AvroMessage(
                messageId,
                enforcer.enforceAvro(headerMap.getFirst(Headers.CONTENT_TYPE_STRING), messageContent, schema.getSchema()),
                timestamp,
                schema);

        validators.check(topic, message);
        byte[] wrapped = messageContentWrapper.wrapAvro(message.getData(), message.getId(), message.getTimestamp(),
                topic, schema, headersPropagator.extract(toHeadersMap(headerMap)));
        return message.withDataReplaced(wrapped);
    }

    private JsonMessage createJsonMessage(HeaderMap headerMap, Topic topic, String messageId, byte[] messageContent, long timestamp) {
        JsonMessage message = new JsonMessage(messageId, messageContent, timestamp);
        if (topic.isValidationEnabled()) {
            CompiledSchema<JsonSchema> schema = extractSchemaVersion(headerMap)
                    .map(version -> schemaRepository.getJsonSchema(topic, version))
                    .orElse(schemaRepository.getJsonSchema(topic));
            message = new JsonMessage(messageId, messageContent, timestamp, of(schema));
        }
        validators.check(topic, message);
        byte[] wrapped = messageContentWrapper.wrapJson(
                message.getData(), message.getId(), message.getTimestamp(), headersPropagator.extract(toHeadersMap(headerMap)));
        return message.withDataReplaced(wrapped);
    }

    private Optional<SchemaVersion> extractSchemaVersion(HeaderMap headerMap) {
        String schemaVersion = headerMap.getFirst(MessageMetadataHeaders.SCHEMA_VERSION.getName());
        if (schemaVersion == null) {
            return Optional.empty();
        }
        try {
            return of(SchemaVersion.valueOf(Integer.valueOf(schemaVersion)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Map<String, String> toHeadersMap(HeaderMap headerMap) {
        return stream(spliteratorUnknownSize(headerMap.iterator(), 0), false)
                .collect(toMap(
                        h -> h.getHeaderName().toString(),
                        h-> h.getFirst()));
    }
}
