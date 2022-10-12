package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import java.util.Map;

public interface MessageContentWrapper {

    UnwrappedMessageContent unwrapAvro(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion);

    UnwrappedMessageContent unwrapJson(byte[] data);

    byte[] wrapAvro(byte[] data,
                    String id,
                    long timestamp,
                    Topic topic,
                    CompiledSchema<Schema> schema,
                    Map<String, String> externalMetadata);

    byte[] wrapJson(byte[] data, String id, long timestamp, Map<String, String> externalMetadata);
}
