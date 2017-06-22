package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.Counter;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class MessageContentWrapper {

    private static final Logger logger = LoggerFactory.getLogger(MessageContentWrapper.class);

    private final JsonMessageContentWrapper jsonMessageContentWrapper;
    private final AvroMessageContentWrapper avroMessageContentWrapper;
    private final SchemaRepository schemaRepository;

    private final Counter deserializationWithMissedSchemaVersionInPayload;
    private final Counter deserializationErrorsForSchemaVersionAwarePayload;
    private final Counter deserializationErrorsForAnySchemaVersion;
    private final Counter deserializationErrorsForAnyOnlineSchemaVersion;

    @Inject
    public MessageContentWrapper(JsonMessageContentWrapper jsonMessageContentWrapper,
                                 AvroMessageContentWrapper avroMessageContentWrapper,
                                 SchemaRepository schemaRepository,
                                 DeserializationMetrics deserializationMetrics) {
        this.jsonMessageContentWrapper = jsonMessageContentWrapper;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
        this.schemaRepository = schemaRepository;

        deserializationErrorsForSchemaVersionAwarePayload = deserializationMetrics.errorsForSchemaVersionAwarePayload();
        deserializationErrorsForAnySchemaVersion = deserializationMetrics.errorsForAnySchemaVersion();
        deserializationErrorsForAnyOnlineSchemaVersion = deserializationMetrics.errorsForAnyOnlineSchemaVersion();
        deserializationWithMissedSchemaVersionInPayload = deserializationMetrics.missedSchemaVersionInPayload();
    }

    public UnwrappedMessageContent unwrapJson(byte[] data) {
        return jsonMessageContentWrapper.unwrapContent(data);
    }

    public UnwrappedMessageContent unwrapAvro(byte[] data, Topic topic) {
        return isPayloadAwareOfSchemaVersion(data, topic) ? deserializeSchemaVersionAwarePayload(data, topic) :
                tryDeserializingUsingAnySchemaVersion(data, topic);
    }

    private boolean isPayloadAwareOfSchemaVersion(byte[] data, Topic topic) {
        if (topic.isSchemaVersionAwareSerializationEnabled()) {
            if (SchemaAwareSerDe.startsWithMagicByte(data)) {
                return true;
            }
            deserializationWithMissedSchemaVersionInPayload.inc();
        }
        return false;
    }

    private UnwrappedMessageContent deserializeSchemaVersionAwarePayload(byte[] data, Topic topic) {
        try {
            SchemaAwarePayload payload = SchemaAwareSerDe.deserialize(data);
            return avroMessageContentWrapper.unwrapContent(payload.getPayload(),
                    schemaRepository.getAvroSchema(topic, payload.getSchemaVersion()));
        } catch (Exception ex) {
            logger.warn("Could not deserialize schema version aware payload for topic {}. Trying to deserialize using any schema version",
                    topic.getQualifiedName(), ex);
            deserializationErrorsForSchemaVersionAwarePayload.inc();
            return tryDeserializingUsingAnySchemaVersion(data, topic);
        }
    }

    // try-harding to find proper schema
    private UnwrappedMessageContent tryDeserializingUsingAnySchemaVersion(byte[] data, Topic topic) {
        try {
            return tryDeserializingUsingAnySchemaVersion(data, topic, false);
        } catch (Exception ex) {
            logger.info("Trying to find schema online for message for topic {}", topic.getQualifiedName());
            return tryDeserializingUsingAnySchemaVersion(data, topic, true);
        }
    }

    private UnwrappedMessageContent tryDeserializingUsingAnySchemaVersion(byte[] data, Topic topic, boolean online) {
        List<SchemaVersion> versions = schemaRepository.getVersions(topic, online);
        for (SchemaVersion version : versions) {
            try {
                CompiledSchema<Schema> schema = online ? schemaRepository.getKnownAvroSchemaVersion(topic, version) :
                        schemaRepository.getAvroSchema(topic, version);
                return avroMessageContentWrapper.unwrapContent(data, schema);
            } catch (Exception ex) {
                logger.debug("Failed to match schema for message for topic {}, schema version {}, fallback to previous.",
                        topic.getQualifiedName(), version.value());
            }
        }
        logger.error("Could not match schema from cache for message for topic {} {}",
                topic.getQualifiedName(), SchemaVersion.toString(versions));
        deserializationErrorsCounterForAnySchemaVersion(online).inc();
        throw new SchemaMissingException(topic);
    }

    private Counter deserializationErrorsCounterForAnySchemaVersion(boolean online) {
        return online ? deserializationErrorsForAnyOnlineSchemaVersion : deserializationErrorsForAnySchemaVersion;
    }

    public byte[] wrapAvro(byte[] data, String id, long timestamp, Topic topic, CompiledSchema<Schema> schema, Map<String, String> externalMetadata) {
        byte[] wrapped = avroMessageContentWrapper.wrapContent(data, id, timestamp, schema.getSchema(), externalMetadata);
        return topic.isSchemaVersionAwareSerializationEnabled() ? SchemaAwareSerDe.serialize(schema.getVersion(), wrapped) : wrapped;
    }

    public byte[] wrapJson(byte[] data, String id, long timestamp, Map<String, String> externalMetadata) {
        return jsonMessageContentWrapper.wrapContent(data, id, timestamp, externalMetadata);
    }
}
