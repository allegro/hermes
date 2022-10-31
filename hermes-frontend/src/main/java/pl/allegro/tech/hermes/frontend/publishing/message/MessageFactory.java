package pl.allegro.tech.hermes.frontend.publishing.message;

import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.http.MessageMetadataHeaders;
import pl.allegro.tech.hermes.common.message.wrapper.AvroInvalidMetadataException;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.common.message.wrapper.WrappingException;
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage;
import pl.allegro.tech.hermes.frontend.publishing.handlers.AttachmentContent;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import tech.allegro.schema.json2avro.converter.AvroConversionException;

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
    private final AvroEnforcer enforcer;
    private final SchemaRepository schemaRepository;
    private final HeadersPropagator headersPropagator;
    private final MessageContentWrapper messageContentWrapper;
    private final Clock clock;
    private final boolean schemaIdHeaderEnabled;

    public MessageFactory(MessageValidators validators,
                          AvroEnforcer enforcer,
                          SchemaRepository schemaRepository,
                          HeadersPropagator headersPropagator,
                          MessageContentWrapper messageContentWrapper,
                          Clock clock,
                          boolean schemaIdHeaderEnabled) {
        this.validators = validators;
        this.enforcer = enforcer;
        this.messageContentWrapper = messageContentWrapper;
        this.schemaRepository = schemaRepository;
        this.headersPropagator = headersPropagator;
        this.clock = clock;
        this.schemaIdHeaderEnabled = schemaIdHeaderEnabled;
    }

    public Message create(HeaderMap headerMap, AttachmentContent attachment) {
        return create(
                headerMap,
                attachment.getTopic(),
                attachment.getMessageId(),
                attachment.getMessageContent());
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
                    } catch (WrappingException | AvroInvalidMetadataException exception) {
                        logger.warn("Unsuccessful wrapping of AVRO message on topic {} in dry run mode",
                                topic.getQualifiedName(), exception);
                    }
                }
                return createJsonMessage(headerMap, messageId, messageContent, timestamp);
            }
            case AVRO:
                return createAvroMessage(headerMap, topic, messageId, messageContent, timestamp);
            default:
                throw new UnsupportedContentTypeException(topic);
        }
    }

    private JsonMessage createJsonMessage(HeaderMap headerMap, String messageId, byte[] messageContent, long timestamp) {
        Map<String, String> extraRequestHeaders = headersPropagator.extract(toHeadersMap(headerMap));
        JsonMessage message = new JsonMessage(messageId, messageContent, timestamp, extractPartitionKey(headerMap),
                extraRequestHeaders);
        byte[] wrapped = messageContentWrapper
                .wrapJson(message.getData(), message.getId(), message.getTimestamp(), extraRequestHeaders);
        return message.withDataReplaced(wrapped);
    }

    private CompiledSchema<Schema> getCompiledSchemaBySchemaVersion(HeaderMap headerMap, Topic topic) {
        return extractSchemaVersion(headerMap)
                .map(version -> schemaRepository.getAvroSchema(topic, version))
                .orElseGet(() -> schemaRepository.getLatestAvroSchema(topic));
    }

    private CompiledSchema<Schema> getCompiledSchema(HeaderMap headerMap, Topic topic) {
        return extractSchemaId(headerMap)
                .map(id -> schemaRepository.getAvroSchema(topic, id))
                .orElseGet(() -> getCompiledSchemaBySchemaVersion(headerMap, topic));
    }

    private AvroMessage createAvroMessage(HeaderMap headerMap, Topic topic, String messageId, byte[] messageContent, long timestamp) {
        CompiledSchema<Schema> schema = getCompiledSchema(headerMap, topic);
        Map<String, String> extraRequestHeaders = headersPropagator.extract(toHeadersMap(headerMap));

        AvroMessage message = new AvroMessage(
                messageId,
                enforcer.enforceAvro(headerMap.getFirst(Headers.CONTENT_TYPE_STRING), messageContent, schema.getSchema(), topic),
                timestamp,
                schema,
                extractPartitionKey(headerMap),
                extraRequestHeaders);

        validators.check(topic, message);
        byte[] wrapped = messageContentWrapper.wrapAvro(message.getData(), message.getId(), message.getTimestamp(),
                topic, schema, extraRequestHeaders);
        return message.withDataReplaced(wrapped);
    }

    private Optional<SchemaVersion> extractSchemaVersion(HeaderMap headerMap) {
        String schemaVersion = headerMap.getFirst(MessageMetadataHeaders.SCHEMA_VERSION.getName());

        if (schemaVersion == null) {
            return Optional.empty();
        }
        try {
            return of(SchemaVersion.valueOf(Integer.parseInt(schemaVersion)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<SchemaId> extractSchemaId(HeaderMap headerMap) {
        if (!schemaIdHeaderEnabled) {
            return Optional.empty();
        }

        String schemaId = headerMap.getFirst(MessageMetadataHeaders.SCHEMA_ID.getName());

        if (schemaId == null) {
            return Optional.empty();
        }

        try {
            return of(SchemaId.valueOf(Integer.parseInt(schemaId)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String extractPartitionKey(HeaderMap headerMap) {
        return headerMap.getFirst(MessageMetadataHeaders.PARTITION_KEY.getName());
    }

    private static Map<String, String> toHeadersMap(HeaderMap headerMap) {
        return stream(spliteratorUnknownSize(headerMap.iterator(), 0), false)
                .collect(toMap(
                        h -> h.getHeaderName().toString(),
                        HeaderValues::getFirst));
    }
}
