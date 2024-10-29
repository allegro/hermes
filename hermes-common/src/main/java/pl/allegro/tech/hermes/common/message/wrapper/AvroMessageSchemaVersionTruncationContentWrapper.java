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

public class AvroMessageSchemaVersionTruncationContentWrapper
    implements AvroMessageContentUnwrapper {

  private static final Logger logger =
      LoggerFactory.getLogger(AvroMessageSchemaVersionTruncationContentWrapper.class);

  private final SchemaRepository schemaRepository;
  private final AvroMessageContentWrapper avroMessageContentWrapper;
  private final boolean magicByteTruncationEnabled;

  private final HermesCounter deserializationWithSchemaVersionTruncation;
  private final HermesCounter deserializationErrorsWithSchemaVersionTruncation;

  public AvroMessageSchemaVersionTruncationContentWrapper(
      SchemaRepository schemaRepository,
      AvroMessageContentWrapper avroMessageContentWrapper,
      MetricsFacade metrics,
      boolean schemaVersionTruncationEnabled) {
    this.schemaRepository = schemaRepository;
    this.avroMessageContentWrapper = avroMessageContentWrapper;
    this.magicByteTruncationEnabled = schemaVersionTruncationEnabled;

    this.deserializationErrorsWithSchemaVersionTruncation =
        metrics.deserialization().errorsForSchemaVersionTruncation();
    this.deserializationWithSchemaVersionTruncation =
        metrics.deserialization().usingSchemaVersionTruncation();
  }

  @Override
  public AvroMessageContentUnwrapperResult unwrap(
      byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
    try {
      deserializationWithSchemaVersionTruncation.increment();

      byte[] dataWithoutMagicByteAndSchemaId = SchemaAwareSerDe.trimMagicByteAndSchemaVersion(data);
      CompiledSchema<Schema> avroSchema =
          schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(schemaVersion));

      return AvroMessageContentUnwrapperResult.success(
          avroMessageContentWrapper.unwrapContent(dataWithoutMagicByteAndSchemaId, avroSchema));
    } catch (Exception e) {
      logger.warn(
          "Could not unwrap content for topic [{}] using schema id provided in header [{}] - falling back",
          topic.getQualifiedName(),
          schemaVersion,
          e);

      deserializationErrorsWithSchemaVersionTruncation.increment();

      return AvroMessageContentUnwrapperResult.failure();
    }
  }

  @Override
  public boolean isApplicable(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
    return magicByteTruncationEnabled
        && containsSchemaVersionInMagicByteAndInHeader(data, schemaVersion);
  }

  private boolean containsSchemaVersionInMagicByteAndInHeader(byte[] data, Integer schemaVersion) {
    return SchemaAwareSerDe.startsWithMagicByte(data) && schemaVersion != null;
  }
}
