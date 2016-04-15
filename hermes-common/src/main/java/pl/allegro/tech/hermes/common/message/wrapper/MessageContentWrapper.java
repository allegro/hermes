package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.serialization.SchemaAwarePayload;
import pl.allegro.tech.hermes.common.message.serialization.SchemaAwareSerDe;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaMissingException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.of;

public class MessageContentWrapper {
    private static final Logger logger = LoggerFactory.getLogger(MessageContentWrapper.class);

    private final JsonMessageContentWrapper jsonMessageContentWrapper;
    private final AvroMessageContentWrapper avroMessageContentWrapper;

    @Inject
    public MessageContentWrapper(JsonMessageContentWrapper jsonMessageContentWrapper,
                                 AvroMessageContentWrapper avroMessageContentWrapper) {
        this.jsonMessageContentWrapper = jsonMessageContentWrapper;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
    }

    public UnwrappedMessageContent unwrapJson(byte[] data) {
        return jsonMessageContentWrapper.unwrapContent(data);
    }

    public UnwrappedMessageContent unwrapAvro(byte[] data,
                                              Topic topic,
                                              Function<Optional<SchemaVersion>, CompiledSchema<Schema>> schemaProvider,
                                              Function<Topic, List<SchemaVersion>> schemaVersionsProvider) {
        return topic.isSchemaVersionAwareSerializationEnabled() ?
                deserialize(data, schemaProvider) : unwrapAvroWithFallback(data, topic, schemaProvider, schemaVersionsProvider);
    }

    private UnwrappedMessageContent unwrapAvroWithFallback(byte[] data,
                                                           Topic topic,
                                                           Function<Optional<SchemaVersion>, CompiledSchema<Schema>> schemaProvider,
                                                           Function<Topic, List<SchemaVersion>> schemaVersionsProvider) {
        List<SchemaVersion> availableVersions = schemaVersionsProvider.apply(topic);
        for (SchemaVersion version : availableVersions) {
            try {
                return avroMessageContentWrapper.unwrapContent(data, schemaProvider.apply(Optional.of(version)));
            } catch (Exception ex) {
                logger.debug("Could not unwrap message for topic {}, schema version {}, fallback to previous.", topic.getQualifiedName(), version.value());
            }
        }
        logger.error("Could not unwrap message for topic {} with any known schema {}", topic.getQualifiedName(), SchemaVersion.toString(availableVersions));
        throw new SchemaMissingException(topic);
    }


    private UnwrappedMessageContent deserialize(byte[] data, Function<Optional<SchemaVersion>, CompiledSchema<Schema>> schemaProvider) {
        SchemaAwarePayload payload = SchemaAwareSerDe.deserialize(data);
        return avroMessageContentWrapper.unwrapContent(payload.getPayload(), schemaProvider.apply(of(payload.getSchemaVersion())));
    }

    public byte[] wrapAvro(byte[] data, String id, long timestamp, Topic topic, CompiledSchema<Schema> schema, Map<String, String> externalMetadata) {
        byte[] wrapped = avroMessageContentWrapper.wrapContent(data, id, timestamp, schema.getSchema(), externalMetadata);
        return topic.isSchemaVersionAwareSerializationEnabled() ? SchemaAwareSerDe.serialize(schema.getVersion(), wrapped) : wrapped;
    }

    public byte[] wrapJson(byte[] data, String id, long timestamp, Map<String, String> externalMetadata) {
        return jsonMessageContentWrapper.wrapContent(data, id, timestamp, externalMetadata);
    }
}
