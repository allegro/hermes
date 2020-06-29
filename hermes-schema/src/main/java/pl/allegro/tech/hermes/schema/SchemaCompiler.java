package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.SchemaWithId;

@FunctionalInterface
public interface SchemaCompiler<T> {

    T compile(SchemaWithId schemaWithId);

}
