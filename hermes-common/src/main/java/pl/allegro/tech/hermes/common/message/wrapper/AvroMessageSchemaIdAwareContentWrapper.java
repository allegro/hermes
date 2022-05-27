package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.Counter;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaRepository;

public class AvroMessageSchemaIdAwareContentWrapper implements AvroMessageContentUnwrapper {

    private static final Logger logger = LoggerFactory.getLogger(AvroMessageSchemaIdAwareContentWrapper.class);

    private final SchemaRepository schemaRepository;
    private final AvroMessageContentWrapper avroMessageContentWrapper;

    private final Counter deserializationUsingSchemaIdAware;
    private final Counter deserializationErrorsForSchemaIdAwarePayload;
    private final Counter deserializationWithMissedSchemaIdInPayload;

    public AvroMessageSchemaIdAwareContentWrapper(SchemaRepository schemaRepository,
                                                  AvroMessageContentWrapper avroMessageContentWrapper,
                                                  DeserializationMetrics deserializationMetrics) {
        this.schemaRepository = schemaRepository;
        this.avroMessageContentWrapper = avroMessageContentWrapper;

        this.deserializationErrorsForSchemaIdAwarePayload = deserializationMetrics.errorsForSchemaIdAwarePayload();
        this.deserializationWithMissedSchemaIdInPayload = deserializationMetrics.missedSchemaIdInPayload();
        this.deserializationUsingSchemaIdAware = deserializationMetrics.usingSchemaIdAware();
    }

    @Override
    public AvroMessageContentUnwrapperResult unwrap(byte[] data, Topic topic, Integer schemaIdFromHeader, Integer schemaVersionFromHeader) {
        try {
            deserializationUsingSchemaIdAware.inc();

            SchemaAwarePayload payload = SchemaAwareSerDe.deserialize(data);
            CompiledSchema<Schema> avroSchema = schemaRepository.getAvroSchema(topic, payload.getSchemaId());

            return AvroMessageContentUnwrapperResult.success(avroMessageContentWrapper.unwrapContent(payload.getPayload(), avroSchema));
        } catch (Exception ex) {
            logger.warn("Could not deserialize schema id aware payload for topic [{}] - falling back", topic.getQualifiedName(), ex);

            deserializationErrorsForSchemaIdAwarePayload.inc();

            return AvroMessageContentUnwrapperResult.failure();
        }
    }

    @Override
    public boolean isApplicable(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
        return isPayloadAwareOfSchemaId(data, topic);
    }

    private boolean isPayloadAwareOfSchemaId(byte[] data, Topic topic) {
        if (topic.isSchemaIdAwareSerializationEnabled()) {
            if (SchemaAwareSerDe.startsWithMagicByte(data)) {
                return true;
            }

            deserializationWithMissedSchemaIdInPayload.inc();
        }

        return false;
    }
}
