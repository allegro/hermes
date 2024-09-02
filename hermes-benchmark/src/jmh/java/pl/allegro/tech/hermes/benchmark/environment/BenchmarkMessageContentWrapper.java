package pl.allegro.tech.hermes.benchmark.environment;

import java.util.Map;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.SchemaAwareSerDe;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;
import pl.allegro.tech.hermes.schema.CompiledSchema;

public class BenchmarkMessageContentWrapper implements MessageContentWrapper {

  private final AvroMessageContentWrapper avroMessageContentWrapper;

  public BenchmarkMessageContentWrapper(AvroMessageContentWrapper avroMessageContentWrapper) {
    this.avroMessageContentWrapper = avroMessageContentWrapper;
  }

  @Override
  public UnwrappedMessageContent unwrapAvro(
      byte[] data, Topic topic, Integer schemaId, Integer schemaVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public UnwrappedMessageContent unwrapJson(byte[] data) {
    throw new UnsupportedOperationException();
  }

  @Override
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

  @Override
  public byte[] wrapJson(
      byte[] data, String id, long timestamp, Map<String, String> externalMetadata) {
    throw new UnsupportedOperationException();
  }
}
