package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.Topic;

public interface CompiledSchemaRepository<T> {

    CompiledSchema<T> getSchema(Topic topic, SchemaVersion version);
}
