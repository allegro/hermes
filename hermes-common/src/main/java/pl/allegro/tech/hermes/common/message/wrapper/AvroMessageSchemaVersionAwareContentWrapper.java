package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.Counter;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaRepository;

import javax.inject.Inject;

public class AvroMessageSchemaVersionAwareContentWrapper {

    private static final Logger logger = LoggerFactory.getLogger(AvroMessageSchemaVersionAwareContentWrapper.class);

    private final SchemaRepository schemaRepository;
    private final AvroMessageContentWrapper avroMessageContentWrapper;
    private final AvroMessageAnySchemaVersionContentWrapper fallbackWrapper;

    private final Counter deserializationErrorsForSchemaVersionAwarePayload;
    private final Counter deserializationWithMissedSchemaVersionInPayload;

    @Inject
    public AvroMessageSchemaVersionAwareContentWrapper(SchemaRepository schemaRepository,
                                                       AvroMessageContentWrapper avroMessageContentWrapper,
                                                       AvroMessageAnySchemaVersionContentWrapper fallbackWrapper,
                                                       DeserializationMetrics deserializationMetrics) {
        this.schemaRepository = schemaRepository;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
        this.fallbackWrapper = fallbackWrapper;

        this.deserializationErrorsForSchemaVersionAwarePayload = deserializationMetrics.errorsForSchemaVersionAwarePayload();
        this.deserializationWithMissedSchemaVersionInPayload = deserializationMetrics.missedSchemaVersionInPayload();
    }

    public UnwrappedMessageContent unwrap(byte[] data, Topic topic) {
        if (!isPayloadAwareOfSchemaVersion(data, topic)) {
            return fallbackWrapper.unwrap(data, topic);
        }

        try {
            SchemaAwarePayload payload = SchemaAwareSerDe.deserialize(data);
            CompiledSchema<Schema> avroSchema = schemaRepository.getAvroSchema(topic, payload.getSchemaVersion());

            return avroMessageContentWrapper.unwrapContent(payload.getPayload(), avroSchema);
        } catch (Exception ex) {
            logger.warn("Could not deserialize schema version aware payload for topic [{}]. Trying fallback deserialization with [{}]",
                    topic.getQualifiedName(), fallbackWrapper.getClass().getSimpleName(), ex);

            deserializationErrorsForSchemaVersionAwarePayload.inc();

            return fallbackWrapper.unwrap(data, topic);
        }
    }

    boolean isPayloadAwareOfSchemaVersion(byte[] data, Topic topic) {
        if (topic.isSchemaVersionAwareSerializationEnabled()) {
            if (SchemaAwareSerDe.startsWithMagicByte(data)) {
                return true;
            }

            deserializationWithMissedSchemaVersionInPayload.inc();
        }

        return false;
    }
}
