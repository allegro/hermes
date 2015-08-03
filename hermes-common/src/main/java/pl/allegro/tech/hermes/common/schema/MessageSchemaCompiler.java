package pl.allegro.tech.hermes.common.schema;

@FunctionalInterface
public interface MessageSchemaCompiler<T> {

    T compile(String schemaSource);

}
