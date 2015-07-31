package pl.allegro.tech.hermes.frontend.schema;

@FunctionalInterface
public interface MessageSchemaCompiler<T> {

    T compile(String schemaSource);

}
