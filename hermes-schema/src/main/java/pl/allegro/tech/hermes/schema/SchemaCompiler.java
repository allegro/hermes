package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.SchemaData;

@FunctionalInterface
public interface SchemaCompiler<T> {

    T compile(SchemaData schemaData);

}
