package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaRepository;

public class AvroMessageSchemaIdAwareContentWrapper implements AvroMessageContentUnwrapper {

  private static final Logger logger =
      LoggerFactory.getLogger(AvroMessageSchemaIdAwareContentWrapper.class);

  private final SchemaRepository schemaRepository;
  private final AvroMessageContentWrapper avroMessageContentWrapper;

  private final HermesCounter deserializationUsingSchemaIdAware;
  private final HermesCounter deserializationErrorsForSchemaIdAwarePayload;
  private final HermesCounter deserializationWithMissingSchemaIdInPayload;

  public AvroMessageSchemaIdAwareContentWrapper(
      SchemaRepository schemaRepository,
      AvroMessageContentWrapper avroMessageContentWrapper,
      MetricsFacade metrics) {
    this.schemaRepository = schemaRepository;
    this.avroMessageContentWrapper = avroMessageContentWrapper;

    this.deserializationErrorsForSchemaIdAwarePayload =
        metrics.deserialization().errorsForSchemaIdAwarePayload();
    this.deserializationWithMissingSchemaIdInPayload =
        metrics.deserialization().missingSchemaIdInPayload();
    this.deserializationUsingSchemaIdAware = metrics.deserialization().usingSchemaIdAware();
  }

  @Override
  public AvroMessageContentUnwrapperResult unwrap(
      byte[] data, Topic topic, Integer schemaIdFromHeader, Integer schemaVersionFromHeader) {
    try {
      deserializationUsingSchemaIdAware.increment();

      SchemaAwarePayload payload = SchemaAwareSerDe.deserialize(data);
      CompiledSchema<Schema> avroSchema =
          schemaRepository.getAvroSchema(topic, payload.getSchemaId());

      return AvroMessageContentUnwrapperResult.success(
          avroMessageContentWrapper.unwrapContent(payload.getPayload(), avroSchema));
    } catch (Exception ex) {
      logger.warn(
          "Could not deserialize schema id aware payload for topic [{}] - falling back",
          topic.getQualifiedName(),
          ex);

      deserializationErrorsForSchemaIdAwarePayload.increment();

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

      deserializationWithMissingSchemaIdInPayload.increment();
    }

    return false;
  }
}
