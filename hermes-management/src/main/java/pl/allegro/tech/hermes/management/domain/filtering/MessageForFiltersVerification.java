package pl.allegro.tech.hermes.management.domain.filtering;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.domain.filtering.FilterableMessage;
import pl.allegro.tech.hermes.schema.CompiledSchema;

class MessageForFiltersVerification implements FilterableMessage {
  private final byte[] data;
  private final ContentType contentType;
  private final CompiledSchema<Schema> schema;

  MessageForFiltersVerification(
      byte[] data, ContentType contentType, CompiledSchema<Schema> schema) {
    this.data = data;
    this.contentType = contentType;
    this.schema = schema;
  }

  @Override
  public ContentType getContentType() {
    return contentType;
  }

  @Override
  public Map<String, String> getExternalMetadata() {
    return Collections.emptyMap();
  }

  @Override
  public byte[] getData() {
    return data;
  }

  @Override
  public Optional<CompiledSchema<Schema>> getSchema() {
    return Optional.ofNullable(schema);
  }
}
