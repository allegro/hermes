package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.Counter;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import javax.inject.Inject;

public class AvroMessageSchemaVersionTruncationContentWrapper implements AvroMessageContentUnwrapper {

    private static final Logger logger = LoggerFactory.getLogger(AvroMessageSchemaVersionTruncationContentWrapper.class);
    private static final int NUMBER_OF_BYTES_TO_TRIM = 5;

    private final SchemaRepository schemaRepository;
    private final AvroMessageContentWrapper avroMessageContentWrapper;
    private final boolean magicByteTruncationEnabled;

    private final Counter deserializationWithSchemaVersionTruncation;
    private final Counter deserializationErrorsWithSchemaVersionTruncation;

    @Inject
    public AvroMessageSchemaVersionTruncationContentWrapper(SchemaRepository schemaRepository,
                                                            AvroMessageContentWrapper avroMessageContentWrapper,
                                                            DeserializationMetrics deserializationMetrics,
                                                            ConfigFactory configFactory) {
        this(
            schemaRepository,
            avroMessageContentWrapper,
            deserializationMetrics,
            configFactory.getBooleanProperty(Configs.SCHEMA_VERSION_TRUNCATION_ENABLED)
        );
    }

    public AvroMessageSchemaVersionTruncationContentWrapper(SchemaRepository schemaRepository,
                                                            AvroMessageContentWrapper avroMessageContentWrapper,
                                                            DeserializationMetrics deserializationMetrics,
                                                            boolean magicByteTruncationEnabled) {
        this.schemaRepository = schemaRepository;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
        this.magicByteTruncationEnabled = magicByteTruncationEnabled;

        this.deserializationErrorsWithSchemaVersionTruncation = deserializationMetrics.errorsForSchemaVersionTruncation();
        this.deserializationWithSchemaVersionTruncation = deserializationMetrics.usingSchemaVersionTruncation();
    }


    @Override
    public AvroMessageContentUnwrapperResult unwrap(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
        try {
            deserializationWithSchemaVersionTruncation.inc();

            byte[] dataWithoutMagicByteAndSchemaId = trimMagicByteAndSchemaVersion(data);
            CompiledSchema<Schema> avroSchema = schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(schemaVersion));

            return AvroMessageContentUnwrapperResult.success(avroMessageContentWrapper.unwrapContent(dataWithoutMagicByteAndSchemaId, avroSchema));
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

    private byte[] trimMagicByteAndSchemaVersion(byte[] data) {
        int length = data.length - NUMBER_OF_BYTES_TO_TRIM;
        byte[] dataWithoutMagicByteAndSchemaVersion = new byte[length];
        System.arraycopy(data, NUMBER_OF_BYTES_TO_TRIM, dataWithoutMagicByteAndSchemaVersion, 0, length);
        return dataWithoutMagicByteAndSchemaVersion;
    }
}
