package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.Counter;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;

public class AvroMessageSchemaVersionTruncationContentWrapper implements AvroMessageContentUnwrapper {

    private static final Logger logger = LoggerFactory.getLogger(AvroMessageSchemaVersionTruncationContentWrapper.class);

    private final SchemaRepository schemaRepository;
    private final AvroMessageContentWrapper avroMessageContentWrapper;
    private final boolean magicByteTruncationEnabled;

    private final Counter deserializationWithSchemaVersionTruncation;
    private final Counter deserializationErrorsWithSchemaVersionTruncation;

    public AvroMessageSchemaVersionTruncationContentWrapper(SchemaRepository schemaRepository,
                                                            AvroMessageContentWrapper avroMessageContentWrapper,
                                                            DeserializationMetrics deserializationMetrics,
                                                            boolean schemaVersionTruncationEnabled) {
        this.schemaRepository = schemaRepository;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
        this.magicByteTruncationEnabled = schemaVersionTruncationEnabled;

        this.deserializationErrorsWithSchemaVersionTruncation = deserializationMetrics.errorsForSchemaVersionTruncation();
        this.deserializationWithSchemaVersionTruncation = deserializationMetrics.usingSchemaVersionTruncation();
    }


    @Override
    public AvroMessageContentUnwrapperResult unwrap(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
        try {
            deserializationWithSchemaVersionTruncation.inc();

            byte[] dataWithoutMagicByteAndSchemaId = SchemaAwareSerDe.trimMagicByteAndSchemaVersion(data);
            CompiledSchema<Schema> avroSchema = schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(schemaVersion));

            return AvroMessageContentUnwrapperResult.success(
                    avroMessageContentWrapper.unwrapContent(dataWithoutMagicByteAndSchemaId, avroSchema));
        } catch (Exception e) {
            logger.warn(
                    "Could not unwrap content for topic [{}] using schema id provided in header [{}] - falling back",
                    topic.getQualifiedName(), schemaVersion, e);

            deserializationErrorsWithSchemaVersionTruncation.inc();

            return AvroMessageContentUnwrapperResult.failure();
        }
    }

    @Override
    public boolean isApplicable(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
        return magicByteTruncationEnabled && containsSchemaVersionInMagicByteAndInHeader(data, schemaVersion);
    }

    private boolean containsSchemaVersionInMagicByteAndInHeader(byte[] data, Integer schemaVersion) {
        return SchemaAwareSerDe.startsWithMagicByte(data) && schemaVersion != null;
    }
}
