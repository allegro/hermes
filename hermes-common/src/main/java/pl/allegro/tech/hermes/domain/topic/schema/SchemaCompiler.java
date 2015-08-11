package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.SchemaSource;

@FunctionalInterface
public interface SchemaCompiler<T> {

    T compile(SchemaSource schemaSource);

}
