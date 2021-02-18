package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.Counter;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaRepository;

import javax.inject.Inject;

public class AvroMessageHeaderSchemaIdContentWrapper implements AvroMessageContentUnwrapper {

    private static final Logger logger = LoggerFactory.getLogger(AvroMessageHeaderSchemaIdContentWrapper.class);

    private final SchemaRepository schemaRepository;
    private final AvroMessageContentWrapper avroMessageContentWrapper;

    private final Counter deserializationWithErrorsUsingHeaderSchemaId;
    private final Counter deserializationUsingHeaderSchemaId;
    private final boolean schemaIdHeaderEnabled;

    @Inject
    public AvroMessageHeaderSchemaIdContentWrapper(SchemaRepository schemaRepository,
                                                   AvroMessageContentWrapper avroMessageContentWrapper,
                                                   DeserializationMetrics deserializationMetrics,
                                                   ConfigFactory configFactory) {
        this(
            schemaRepository,
            avroMessageContentWrapper,
            deserializationMetrics,
            configFactory.getBooleanProperty(Configs.SCHEMA_ID_HEADER_ENABLED)
        );
    }

    public AvroMessageHeaderSchemaIdContentWrapper(SchemaRepository schemaRepository,
                                                   AvroMessageContentWrapper avroMessageContentWrapper,
                                                   DeserializationMetrics deserializationMetrics,
                                                   boolean schemaIdHeaderEnabled) {
        this.schemaRepository = schemaRepository;
        this.avroMessageContentWrapper = avroMessageContentWrapper;

        this.deserializationWithErrorsUsingHeaderSchemaId = deserializationMetrics.errorsForHeaderSchemaId();
        this.deserializationUsingHeaderSchemaId = deserializationMetrics.usingHeaderSchemaId();
        this.schemaIdHeaderEnabled = schemaIdHeaderEnabled;
    }

    @Override
    public AvroMessageContentUnwrapperResult unwrap(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
        try {
            deserializationUsingHeaderSchemaId.inc();
            CompiledSchema<Schema> avroSchema = schemaRepository.getAvroSchema(topic, SchemaId.valueOf(schemaId));

            return AvroMessageContentUnwrapperResult.success(avroMessageContentWrapper.unwrapContent(data, avroSchema));
        } catch (Exception ex) {
            logger.warn(
                    "Could not unwrap content for topic [{}] using schema id provided in header [{}] - falling back",
                    topic.getQualifiedName(), schemaVersion, ex);

            deserializationWithErrorsUsingHeaderSchemaId.inc();

            return AvroMessageContentUnwrapperResult.failure();
        }
    }

    @Override
    public boolean isApplicable(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
        return schemaIdHeaderEnabled && schemaId != null;
    }
}
