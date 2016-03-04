package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.serialization.SchemaAwarePayload;
import pl.allegro.tech.hermes.common.message.serialization.SchemaAwareSerDe;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.empty;
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

    public UnwrappedMessageContent unwrapAvro(byte[] data, Topic topic, Function<Optional<SchemaVersion>, CompiledSchema<Schema>> schemaProvider) {
        return topic.isSchemaVersionAwareSerializationEnabled() ?
                deserialize(data, schemaProvider) : avroMessageContentWrapper.unwrapContent(data, schemaProvider.apply(empty()));
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
