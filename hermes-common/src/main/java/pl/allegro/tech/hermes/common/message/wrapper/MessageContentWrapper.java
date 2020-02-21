package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;

import static java.util.Arrays.asList;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentUnwrapperResult.AvroMessageContentUnwrapperResultStatus.SUCCESS;

public class MessageContentWrapper {

    private static final Logger logger = LoggerFactory.getLogger(MessageContentWrapper.class);

    private final JsonMessageContentWrapper jsonMessageContentWrapper;
    private final AvroMessageContentWrapper avroMessageContentWrapper;
    private final Collection<AvroMessageContentUnwrapper> avroMessageContentUnwrappers;

    @Inject
    public MessageContentWrapper(JsonMessageContentWrapper jsonMessageContentWrapper,
                                 AvroMessageContentWrapper avroMessageContentWrapper,
                                 AvroMessageSchemaVersionAwareContentWrapper schemaVersionAwareContentWrapper,
                                 AvroMessageHeaderSchemaVersionContentWrapper headerSchemaVersionContentWrapper,
                                 AvroMessageAnySchemaVersionContentWrapper anySchemaVersionContentWrapper) {

        this.jsonMessageContentWrapper = jsonMessageContentWrapper;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
        this.avroMessageContentUnwrappers =
                asList(schemaVersionAwareContentWrapper, headerSchemaVersionContentWrapper, anySchemaVersionContentWrapper);
    }

    public UnwrappedMessageContent unwrapJson(byte[] data) {
        return jsonMessageContentWrapper.unwrapContent(data);
    }

    public UnwrappedMessageContent unwrapAvro(byte[] data, Topic topic, Integer schemaVersion) {
        for (AvroMessageContentUnwrapper unwrapper : avroMessageContentUnwrappers) {
            if (unwrapper.isApplicable(data, topic, schemaVersion)) {
                AvroMessageContentUnwrapperResult result = unwrapper.unwrap(data, topic, schemaVersion);
                if (result.getStatus() == SUCCESS) {
                    return result.getContent();
                }
            }
        }

        logger.error("All attempts to unwrap Avro message for topic {} with schema version {} failed", topic.getQualifiedName(), schemaVersion);
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
