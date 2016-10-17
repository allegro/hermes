package pl.allegro.tech.hermes.common.message.wrapper;

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

    @Inject
    public MessageContentWrapper(JsonMessageContentWrapper jsonMessageContentWrapper,
                                 AvroMessageContentWrapper avroMessageContentWrapper, SchemaRepository schemaRepository) {
        this.jsonMessageContentWrapper = jsonMessageContentWrapper;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
        this.schemaRepository = schemaRepository;
    }

    public UnwrappedMessageContent unwrapJson(byte[] data) {
        return jsonMessageContentWrapper.unwrapContent(data);
    }

    public UnwrappedMessageContent unwrapAvro(byte[] data,
                                              Topic topic) {
        return topic.isSchemaVersionAwareSerializationEnabled() ? deserializeSchemaVersionAwarePayload(data, topic) :
                tryDeserializingUsingAnySchemaVersion(data, topic);
    }

    private UnwrappedMessageContent deserializeSchemaVersionAwarePayload(byte[] data, Topic topic) {
        SchemaAwarePayload payload = SchemaAwareSerDe.deserialize(data);
        return avroMessageContentWrapper.unwrapContent(payload.getPayload(),
                schemaRepository.getAvroSchema(topic, payload.getSchemaVersion()));
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
        throw new SchemaMissingException(topic);
    }

    public byte[] wrapAvro(byte[] data, String id, long timestamp, Topic topic, CompiledSchema<Schema> schema, Map<String, String> externalMetadata) {
        byte[] wrapped = avroMessageContentWrapper.wrapContent(data, id, timestamp, schema.getSchema(), externalMetadata);
        return topic.isSchemaVersionAwareSerializationEnabled() ? SchemaAwareSerDe.serialize(schema.getVersion(), wrapped) : wrapped;
    }

    public byte[] wrapJson(byte[] data, String id, long timestamp, Map<String, String> externalMetadata) {
        return jsonMessageContentWrapper.wrapContent(data, id, timestamp, externalMetadata);
    }
}
