package pl.allegro.tech.hermes.domain.topic.schema;

@FunctionalInterface
public interface SchemaCompiler<T> {

    T compile(String schemaSource);

}
