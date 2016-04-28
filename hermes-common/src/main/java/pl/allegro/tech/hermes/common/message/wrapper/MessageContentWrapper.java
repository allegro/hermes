package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.AvroSchemaSource;
import pl.allegro.tech.hermes.common.message.serialization.SchemaAwarePayload;
import pl.allegro.tech.hermes.common.message.serialization.SchemaAwareSerDe;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;

import javax.inject.Inject;
import java.util.Map;

import static java.util.Optional.of;

public class MessageContentWrapper {
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
                                              AvroSchemaSource schemaSource) {
        return topic.isSchemaVersionAwareSerializationEnabled() ?
                deserialize(data, topic, schemaSource) : schemaSource.tryHard(topic, schema -> avroMessageContentWrapper.unwrapContent(data, schema));
    }

    private UnwrappedMessageContent deserialize(byte[] data, Topic topic, AvroSchemaSource schemaSource) {
        SchemaAwarePayload payload = SchemaAwareSerDe.deserialize(data);
        return avroMessageContentWrapper.unwrapContent(payload.getPayload(), schemaSource.getAvroSchema(topic, payload.getSchemaVersion()));
    }

    public byte[] wrapAvro(byte[] data, String id, long timestamp, Topic topic, CompiledSchema<Schema> schema, Map<String, String> externalMetadata) {
        byte[] wrapped = avroMessageContentWrapper.wrapContent(data, id, timestamp, schema.getSchema(), externalMetadata);
        return topic.isSchemaVersionAwareSerializationEnabled() ? SchemaAwareSerDe.serialize(schema.getVersion(), wrapped) : wrapped;
    }

    public byte[] wrapJson(byte[] data, String id, long timestamp, Map<String, String> externalMetadata) {
        return jsonMessageContentWrapper.wrapContent(data, id, timestamp, externalMetadata);
    }
}
