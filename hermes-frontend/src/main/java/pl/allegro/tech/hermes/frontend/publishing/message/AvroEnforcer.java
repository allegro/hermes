package pl.allegro.tech.hermes.frontend.publishing.message;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;

public interface AvroEnforcer {

  byte[] enforceAvro(String payloadContentType, byte[] data, Schema schema, Topic topic);
}
