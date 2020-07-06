package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.RawSchema;

@FunctionalInterface
public interface SchemaCompiler<T> {

    T compile(RawSchema rawSchema);

}
