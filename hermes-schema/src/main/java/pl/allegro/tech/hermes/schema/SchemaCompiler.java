package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.SchemaMetadata;

@FunctionalInterface
public interface SchemaCompiler<T> {

    T compile(SchemaMetadata schemaMetadata);

}
