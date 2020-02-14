package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import javax.inject.Inject;
import java.util.Map;

public class MessageContentWrapper {

    private final JsonMessageContentWrapper jsonMessageContentWrapper;
    private final AvroMessageContentWrapper avroMessageContentWrapper;

    private final AvroMessageHeaderSchemaVersionContentWrapper headerSchemaVersionContentWrapper;

    @Inject
    public MessageContentWrapper(JsonMessageContentWrapper jsonMessageContentWrapper,
                                 AvroMessageContentWrapper avroMessageContentWrapper,
                                 AvroMessageHeaderSchemaVersionContentWrapper headerSchemaVersionContentWrapper) {

        this.jsonMessageContentWrapper = jsonMessageContentWrapper;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
        this.headerSchemaVersionContentWrapper = headerSchemaVersionContentWrapper;
    }

    public UnwrappedMessageContent unwrapJson(byte[] data) {
        return jsonMessageContentWrapper.unwrapContent(data);
    }

    public UnwrappedMessageContent unwrapAvro(byte[] data, Topic topic, Integer schemaVersion) {
        return headerSchemaVersionContentWrapper.unwrap(data, topic, schemaVersion);
    }

    public byte[] wrapAvro(byte[] data, String id, long timestamp, Topic topic, CompiledSchema<Schema> schema, Map<String, String> externalMetadata) {
        byte[] wrapped = avroMessageContentWrapper.wrapContent(data, id, timestamp, schema.getSchema(), externalMetadata);
        return topic.isSchemaVersionAwareSerializationEnabled() ? SchemaAwareSerDe.serialize(schema.getVersion(), wrapped) : wrapped;
    }

    public byte[] wrapJson(byte[] data, String id, long timestamp, Map<String, String> externalMetadata) {
        return jsonMessageContentWrapper.wrapContent(data, id, timestamp, externalMetadata);
    }
}
