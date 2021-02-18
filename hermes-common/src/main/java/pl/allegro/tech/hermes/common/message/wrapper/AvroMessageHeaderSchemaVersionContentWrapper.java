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

public class AvroMessageHeaderSchemaVersionContentWrapper implements AvroMessageContentUnwrapper {

    private static final Logger logger = LoggerFactory.getLogger(AvroMessageHeaderSchemaVersionContentWrapper.class);

    private final SchemaRepository schemaRepository;
    private final AvroMessageContentWrapper avroMessageContentWrapper;

    private final Counter deserializationWithErrorsUsingHeaderSchemaVersion;
    private final Counter deserializationUsingHeaderSchemaVersion;

    @Inject
    public AvroMessageHeaderSchemaVersionContentWrapper(SchemaRepository schemaRepository,
                                                        AvroMessageContentWrapper avroMessageContentWrapper,
                                                        DeserializationMetrics deserializationMetrics) {
        this.schemaRepository = schemaRepository;
        this.avroMessageContentWrapper = avroMessageContentWrapper;

        this.deserializationWithErrorsUsingHeaderSchemaVersion = deserializationMetrics.errorsForHeaderSchemaVersion();
        this.deserializationUsingHeaderSchemaVersion = deserializationMetrics.usingHeaderSchemaVersion();
    }

    @Override
    public AvroMessageContentUnwrapperResult unwrap(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
        try {
            deserializationUsingHeaderSchemaVersion.inc();
            CompiledSchema<Schema> avroSchema = schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(schemaVersion));

            return AvroMessageContentUnwrapperResult.success(avroMessageContentWrapper.unwrapContent(data, avroSchema));
        } catch (Exception ex) {
            logger.warn(
                    "Could not unwrap content for topic [{}] using schema version provided in header [{}] - falling back",
                    topic.getQualifiedName(), schemaVersion, ex);

            deserializationWithErrorsUsingHeaderSchemaVersion.inc();

            return AvroMessageContentUnwrapperResult.failure();
        }
    }

    @Override
    public boolean isApplicable(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
        return schemaVersion != null;
    }
}
