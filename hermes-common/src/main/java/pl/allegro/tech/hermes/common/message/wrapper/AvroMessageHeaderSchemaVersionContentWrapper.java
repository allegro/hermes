package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;

public class AvroMessageHeaderSchemaVersionContentWrapper implements AvroMessageContentUnwrapper {

  private static final Logger logger =
      LoggerFactory.getLogger(AvroMessageHeaderSchemaVersionContentWrapper.class);

  private final SchemaRepository schemaRepository;
  private final AvroMessageContentWrapper avroMessageContentWrapper;

  private final HermesCounter deserializationWithErrorsUsingHeaderSchemaVersion;
  private final HermesCounter deserializationUsingHeaderSchemaVersion;

  public AvroMessageHeaderSchemaVersionContentWrapper(
      SchemaRepository schemaRepository,
      AvroMessageContentWrapper avroMessageContentWrapper,
      MetricsFacade metrics) {
    this.schemaRepository = schemaRepository;
    this.avroMessageContentWrapper = avroMessageContentWrapper;

    this.deserializationWithErrorsUsingHeaderSchemaVersion =
        metrics.deserialization().errorsForHeaderSchemaVersion();
    this.deserializationUsingHeaderSchemaVersion =
        metrics.deserialization().usingHeaderSchemaVersion();
  }

  @Override
  public AvroMessageContentUnwrapperResult unwrap(
      byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
    try {
      deserializationUsingHeaderSchemaVersion.increment();
      CompiledSchema<Schema> avroSchema =
          schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(schemaVersion));

      return AvroMessageContentUnwrapperResult.success(
          avroMessageContentWrapper.unwrapContent(data, avroSchema));
    } catch (Exception ex) {
      logger.warn(
          "Could not unwrap content for topic [{}] using schema version provided in header [{}] - falling back",
          topic.getQualifiedName(),
          schemaVersion,
          ex);

      deserializationWithErrorsUsingHeaderSchemaVersion.increment();

      return AvroMessageContentUnwrapperResult.failure();
    }
  }

  @Override
  public boolean isApplicable(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
    return schemaVersion != null;
  }
}
