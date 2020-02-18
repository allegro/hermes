package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.Counter;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaRepository;

import javax.inject.Inject;

public class AvroMessageSchemaVersionAwareContentWrapper implements AvroMessageContentUnwrapper {

    private static final Logger logger = LoggerFactory.getLogger(AvroMessageSchemaVersionAwareContentWrapper.class);

    private final SchemaRepository schemaRepository;
    private final AvroMessageContentWrapper avroMessageContentWrapper;

    private final Counter deserializationUsingSchemaVersionAware;
    private final Counter deserializationErrorsForSchemaVersionAwarePayload;
    private final Counter deserializationWithMissedSchemaVersionInPayload;

    @Inject
    public AvroMessageSchemaVersionAwareContentWrapper(SchemaRepository schemaRepository,
                                                       AvroMessageContentWrapper avroMessageContentWrapper,
                                                       DeserializationMetrics deserializationMetrics) {
        this.schemaRepository = schemaRepository;
        this.avroMessageContentWrapper = avroMessageContentWrapper;

        this.deserializationErrorsForSchemaVersionAwarePayload = deserializationMetrics.errorsForSchemaVersionAwarePayload();
        this.deserializationWithMissedSchemaVersionInPayload = deserializationMetrics.missedSchemaVersionInPayload();
        this.deserializationUsingSchemaVersionAware = deserializationMetrics.usingSchemaVersionAware();
    }

    @Override
    public AvroMessageContentUnwrapperResult unwrap(byte[] data, Topic topic, Integer schemaVersionFromHeader) {
        try {
            deserializationUsingSchemaVersionAware.inc();

            SchemaAwarePayload payload = SchemaAwareSerDe.deserialize(data);
            CompiledSchema<Schema> avroSchema = schemaRepository.getAvroSchema(topic, payload.getSchemaVersion());

            return AvroMessageContentUnwrapperResult.success(avroMessageContentWrapper.unwrapContent(payload.getPayload(), avroSchema));
        } catch (Exception ex) {
            logger.warn("Could not deserialize schema version aware payload for topic [{}] - falling back", topic.getQualifiedName(), ex);

            deserializationErrorsForSchemaVersionAwarePayload.inc();

            return AvroMessageContentUnwrapperResult.failure();
        }
    }

    @Override
    public boolean isApplicable(byte[] data, Topic topic, Integer schemaVersion) {
        return isPayloadAwareOfSchemaVersion(data, topic);
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
}
