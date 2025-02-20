package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.Topic;

public interface CompiledSchemaRepository<T> {

  default CompiledSchema<T> getSchema(Topic topic, SchemaVersion version) {
    return getSchema(topic, version, false);
  }

  CompiledSchema<T> getSchema(Topic topic, SchemaVersion version, boolean online);

  CompiledSchema<T> getSchema(Topic topic, SchemaId id);
}
