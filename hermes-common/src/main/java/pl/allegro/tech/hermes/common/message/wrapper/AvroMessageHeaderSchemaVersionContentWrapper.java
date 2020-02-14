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

public class AvroMessageHeaderSchemaVersionContentWrapper {

    private static final Logger logger = LoggerFactory.getLogger(AvroMessageHeaderSchemaVersionContentWrapper.class);

    private final SchemaRepository schemaRepository;
    private final AvroMessageContentWrapper avroMessageContentWrapper;
    private final AvroMessageSchemaVersionAwareContentWrapper fallbackWrapper;

    private final Counter deserializationWithErrorsForHeaderSchemaVersion;
    private final Counter deserializationWithMissedSchemaVersionInHeader;

    @Inject
    public AvroMessageHeaderSchemaVersionContentWrapper(SchemaRepository schemaRepository,
                                                        AvroMessageContentWrapper avroMessageContentWrapper,
                                                        AvroMessageSchemaVersionAwareContentWrapper fallbackWrapper,
                                                        DeserializationMetrics deserializationMetrics) {
        this.schemaRepository = schemaRepository;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
        this.fallbackWrapper = fallbackWrapper;

        this.deserializationWithErrorsForHeaderSchemaVersion = deserializationMetrics.errorsForHeaderSchemaVersion();
        this.deserializationWithMissedSchemaVersionInHeader = deserializationMetrics.missedSchemaVersionInHeader();

    }

    public UnwrappedMessageContent unwrap(byte[] data, Topic topic, Integer schemaVersion) {
        if (schemaVersion == null) {
            deserializationWithMissedSchemaVersionInHeader.inc();
            return fallbackWrapper.unwrap(data, topic);
        }

        try {
            CompiledSchema<Schema> avroSchema = schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(schemaVersion));

            return avroMessageContentWrapper.unwrapContent(data, avroSchema);
        } catch (Exception ex) {
            logger.warn("Could not unwrap content for topic [{}] using schema version provided in header [{}]. Trying fallback deserialization with [{}]",
                    topic.getQualifiedName(), schemaVersion, fallbackWrapper.getClass().getSimpleName(), ex);

            deserializationWithErrorsForHeaderSchemaVersion.inc();

            return fallbackWrapper.unwrap(data, topic);
        }
    }
}
