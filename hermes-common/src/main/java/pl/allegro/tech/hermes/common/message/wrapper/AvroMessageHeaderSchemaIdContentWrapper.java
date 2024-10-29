package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaRepository;

public class AvroMessageHeaderSchemaIdContentWrapper implements AvroMessageContentUnwrapper {

  private static final Logger logger =
      LoggerFactory.getLogger(AvroMessageHeaderSchemaIdContentWrapper.class);

  private final SchemaRepository schemaRepository;
  private final AvroMessageContentWrapper avroMessageContentWrapper;

  private final HermesCounter deserializationWithErrorsUsingHeaderSchemaId;
  private final HermesCounter deserializationUsingHeaderSchemaId;
  private final boolean schemaIdHeaderEnabled;

  public AvroMessageHeaderSchemaIdContentWrapper(
      SchemaRepository schemaRepository,
      AvroMessageContentWrapper avroMessageContentWrapper,
      MetricsFacade metrics,
      boolean schemaIdHeaderEnabled) {
    this.schemaRepository = schemaRepository;
    this.avroMessageContentWrapper = avroMessageContentWrapper;

    this.deserializationWithErrorsUsingHeaderSchemaId =
        metrics.deserialization().errorsForHeaderSchemaId();
    this.deserializationUsingHeaderSchemaId = metrics.deserialization().usingHeaderSchemaId();
    this.schemaIdHeaderEnabled = schemaIdHeaderEnabled;
  }

  @Override
  public AvroMessageContentUnwrapperResult unwrap(
      byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
    try {
      deserializationUsingHeaderSchemaId.increment();
      CompiledSchema<Schema> avroSchema =
          schemaRepository.getAvroSchema(topic, SchemaId.valueOf(schemaId));

      return AvroMessageContentUnwrapperResult.success(
          avroMessageContentWrapper.unwrapContent(data, avroSchema));
    } catch (Exception ex) {
      logger.warn(
          "Could not unwrap content for topic [{}] using schema id provided in header [{}] - falling back",
          topic.getQualifiedName(),
          schemaVersion,
          ex);

      deserializationWithErrorsUsingHeaderSchemaId.increment();

      return AvroMessageContentUnwrapperResult.failure();
    }
  }

  @Override
  public boolean isApplicable(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
    return schemaIdHeaderEnabled && schemaId != null;
  }
}
