package pl.allegro.tech.hermes.common.message.wrapper;

import static java.util.Arrays.asList;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentUnwrapperResult.AvroMessageContentUnwrapperResultStatus.SUCCESS;

import java.util.Collection;
import java.util.Map;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;

public class CompositeMessageContentWrapper implements MessageContentWrapper {

  private static final Logger logger =
      LoggerFactory.getLogger(CompositeMessageContentWrapper.class);

  private final JsonMessageContentWrapper jsonMessageContentWrapper;
  private final AvroMessageContentWrapper avroMessageContentWrapper;
  private final Collection<AvroMessageContentUnwrapper> avroMessageContentUnwrappers;

  public CompositeMessageContentWrapper(
      JsonMessageContentWrapper jsonMessageContentWrapper,
      AvroMessageContentWrapper avroMessageContentWrapper,
      AvroMessageSchemaIdAwareContentWrapper schemaIdAwareContentWrapper,
      AvroMessageHeaderSchemaVersionContentWrapper headerSchemaVersionContentWrapper,
      AvroMessageHeaderSchemaIdContentWrapper headerSchemaIdContentWrapper,
      AvroMessageSchemaVersionTruncationContentWrapper schemaVersionTruncationContentWrapper) {

    this.jsonMessageContentWrapper = jsonMessageContentWrapper;
    this.avroMessageContentWrapper = avroMessageContentWrapper;
    this.avroMessageContentUnwrappers =
        asList(
            schemaIdAwareContentWrapper,
            schemaVersionTruncationContentWrapper,
            headerSchemaVersionContentWrapper,
            headerSchemaIdContentWrapper);
  }

  public UnwrappedMessageContent unwrapJson(byte[] data) {
    return jsonMessageContentWrapper.unwrapContent(data);
  }

  public UnwrappedMessageContent unwrapAvro(
      byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
    for (AvroMessageContentUnwrapper unwrapper : avroMessageContentUnwrappers) {
      if (unwrapper.isApplicable(data, topic, schemaId, schemaVersion)) {
        AvroMessageContentUnwrapperResult result =
            unwrapper.unwrap(data, topic, schemaId, schemaVersion);
        if (result.getStatus() == SUCCESS) {
          return result.getContent();
        }
      }
    }

    logger.error(
        "All attempts to unwrap Avro message for topic {} with schema version {} failed",
        topic.getQualifiedName(),
        schemaVersion);
    throw new SchemaMissingException(topic);
  }

  public byte[] wrapAvro(
      byte[] data,
      String id,
      long timestamp,
      Topic topic,
      CompiledSchema<Schema> schema,
      Map<String, String> externalMetadata) {
    byte[] wrapped =
        avroMessageContentWrapper.wrapContent(
            data, id, timestamp, schema.getSchema(), externalMetadata);
    return topic.isSchemaIdAwareSerializationEnabled()
        ? SchemaAwareSerDe.serialize(schema.getId(), wrapped)
        : wrapped;
  }

  public byte[] wrapJson(
      byte[] data, String id, long timestamp, Map<String, String> externalMetadata) {
    return jsonMessageContentWrapper.wrapContent(data, id, timestamp, externalMetadata);
  }
}
