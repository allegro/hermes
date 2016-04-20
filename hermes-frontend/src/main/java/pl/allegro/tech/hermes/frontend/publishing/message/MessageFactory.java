package pl.allegro.tech.hermes.frontend.publishing.message;

import com.github.fge.jsonschema.main.JsonSchema;
import com.google.common.collect.ImmutableMap;
import org.apache.avro.Schema;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.http.MessageMetadataHeaders;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;
import pl.allegro.tech.hermes.frontend.publishing.MessageContentTypeEnforcer;
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import tech.allegro.schema.json2avro.converter.AvroConversionException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import java.time.Clock;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

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

    public Message create(HttpServletRequest request, Topic topic, String messageId, byte[] messageContent) {
        long timestamp = clock.millis();
        switch (topic.getContentType()) {
            case JSON: {
                if (topic.isJsonToAvroDryRunEnabled()) {
                    try {
                        createAvroMessage(request, topic, messageId, messageContent, timestamp);
                    } catch (AvroConversionException exception) {
                        logger.warn("Unsuccessful message conversion from JSON to AVRO on topic {} in dry run mode",
                                topic.getQualifiedName(), exception);
                    }
                }
                return createJsonMessage(request, topic, messageId, messageContent, timestamp);
            }
            case AVRO:
                return createAvroMessage(request, topic, messageId, messageContent, timestamp);
            default:
                throw new UnsupportedContentTypeException(topic);
        }
    }

    public Message create(Topic topic, String messageId, byte[] messageContent, long timestamp, Optional<SchemaVersion> schemaVersionOptional) {
        switch (topic.getContentType()) {
            case JSON: {
                if (topic.isJsonToAvroDryRunEnabled()) {
                    try {
                        prepareAvroSchemaAndMessage(schemaVersionOptional, MediaType.APPLICATION_OCTET_STREAM, topic, messageId, messageContent, timestamp);
                    } catch (AvroConversionException exception) {
                        logger.warn("Unsuccessful message conversion from JSON to AVRO on topic {} in dry run mode",
                                topic.getQualifiedName(), exception);
                    }
                }
                return prepareJsonMessage(schemaVersionOptional, topic, messageId, messageContent, timestamp);
            }
            case AVRO:
                return prepareAvroSchemaAndMessage(schemaVersionOptional, MediaType.APPLICATION_OCTET_STREAM, topic, messageId, messageContent, timestamp).getValue();
            default:
                throw new UnsupportedContentTypeException(topic);
        }
    }

    private AvroMessage createAvroMessage(HttpServletRequest request, Topic topic, String messageId, byte[] messageContent, long timestamp) {
        Pair<CompiledSchema<Schema>, AvroMessage> pair = prepareAvroSchemaAndMessage(extractSchemaVersion(request), request.getContentType(), topic, messageId, messageContent, timestamp);

        AvroMessage message = pair.getValue();
        validators.check(topic, message);
        byte[] wrapped = messageContentWrapper.wrapAvro(message.getData(), message.getId(), message.getTimestamp(),
                topic, pair.getKey(), headersPropagator.extract(toHeadersMap(request)));
        return message.withDataReplaced(wrapped);
    }

    @NotNull
    private Pair<CompiledSchema<Schema>, AvroMessage> prepareAvroSchemaAndMessage(Optional<SchemaVersion> schemaVersionOptional, String payloadContentType, Topic topic, String messageId, byte[] messageContent, long timestamp) {
        CompiledSchema<Schema> schema = schemaVersionOptional
                .map(version -> schemaRepository.getAvroSchema(topic, version))
                .orElse(schemaRepository.getAvroSchema(topic));

        return Pair.of(schema, new AvroMessage(
                messageId,
                enforcer.enforceAvro(payloadContentType, messageContent, schema.getSchema()),
                timestamp,
                schema));
    }

    private JsonMessage createJsonMessage(HttpServletRequest request, Topic topic, String messageId, byte[] messageContent, long timestamp) {
        JsonMessage message = prepareJsonMessage(extractSchemaVersion(request), topic, messageId, messageContent, timestamp);
        validators.check(topic, message);
        byte[] wrapped = messageContentWrapper.wrapJson(message.getData(), message.getId(), message.getTimestamp(), headersPropagator.extract(toHeadersMap(request)));
        return message.withDataReplaced(wrapped);
    }

    @NotNull
    private JsonMessage prepareJsonMessage(Optional<SchemaVersion> schemaVersionOptional, Topic topic, String messageId, byte[] messageContent, long timestamp) {
        JsonMessage message = new JsonMessage(messageId, messageContent, timestamp);
        if (topic.isValidationEnabled()) {
            CompiledSchema<JsonSchema> schema = schemaVersionOptional
                    .map(version -> schemaRepository.getJsonSchema(topic, version))
                    .orElse(schemaRepository.getJsonSchema(topic));
            message = new JsonMessage(messageId, messageContent, timestamp, of(schema));
        }
        return message;
    }

    private Optional<SchemaVersion> extractSchemaVersion(HttpServletRequest request) {
        int version = request.getIntHeader(MessageMetadataHeaders.SCHEMA_VERSION.getName());
        return version < 0 ? empty() : of(SchemaVersion.valueOf(version));
    }

    private Map<String, String> toHeadersMap(HttpServletRequest request) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            builder.put(header, request.getHeader(header));
        }
        return builder.build();
    }
}
