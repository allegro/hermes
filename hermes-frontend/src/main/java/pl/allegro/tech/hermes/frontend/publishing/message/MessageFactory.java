package pl.allegro.tech.hermes.frontend.publishing.message;

import com.google.common.collect.ImmutableMap;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.http.MessageMetadataHeaders;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.frontend.publishing.MessageContentTypeEnforcer;
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import tech.allegro.schema.json2avro.converter.AvroConversionException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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
                return createJsonMessage(request, messageId, messageContent, timestamp);
            }
            case AVRO:
                return createAvroMessage(request, topic, messageId, messageContent, timestamp);
            default: throw new UnsupportedContentTypeException(topic);
        }
    }

    private JsonMessage createJsonMessage(HttpServletRequest request, String messageId, byte[] messageContent, long timestamp) {
        JsonMessage message = new JsonMessage(messageId, messageContent, timestamp);
        byte[] wrapped = messageContentWrapper.wrapJson(message.getData(), message.getId(), message.getTimestamp(), headersPropagator.extract(toHeadersMap(request)));
        return message.withDataReplaced(wrapped);
    }

    private AvroMessage createAvroMessage(HttpServletRequest request, Topic topic, String messageId, byte[] messageContent, long timestamp) {
        CompiledSchema<Schema> schema = extractSchemaVersion(request)
                .map(version -> schemaRepository.getAvroSchema(topic, version))
                .orElse(schemaRepository.getLatestAvroSchema(topic));

        AvroMessage message = new AvroMessage(
                messageId,
                enforcer.enforceAvro(request.getContentType(), messageContent, schema.getSchema()),
                timestamp,
                schema);

        validators.check(topic, message);
        byte[] wrapped = messageContentWrapper.wrapAvro(message.getData(), message.getId(), message.getTimestamp(),
                topic, schema, headersPropagator.extract(toHeadersMap(request)));
        return message.withDataReplaced(wrapped);
    }

    private Optional<SchemaVersion> extractSchemaVersion(HttpServletRequest request) {
        int version = request.getIntHeader(MessageMetadataHeaders.SCHEMA_VERSION.getName());
        return version < 0 ? empty() : of(SchemaVersion.valueOf(version));
    }

    private Map<String, String> toHeadersMap(HttpServletRequest request) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
        Enumeration<String> headers = request.getHeaderNames();
        while(headers.hasMoreElements()) {
            String header = headers.nextElement();
            builder.put(header, request.getHeader(header));
        }
        return builder.build();
    }
}
