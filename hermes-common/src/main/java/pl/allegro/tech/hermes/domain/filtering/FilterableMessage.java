package pl.allegro.tech.hermes.domain.filtering;

import java.util.Map;
import java.util.Optional;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.schema.CompiledSchema;

public interface FilterableMessage {

  ContentType getContentType();

  Map<String, String> getExternalMetadata();

  byte[] getData();

  Optional<CompiledSchema<Schema>> getSchema();
}
