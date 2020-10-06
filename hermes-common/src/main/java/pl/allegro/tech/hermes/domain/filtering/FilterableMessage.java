package pl.allegro.tech.hermes.domain.filtering;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import java.util.Map;
import java.util.Optional;

public interface FilterableMessage {

    ContentType getContentType();

    Map<String, String> getExternalMetadata();

    byte[] getData();

    Optional<CompiledSchema<Schema>> getSchema();
}
