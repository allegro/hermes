package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.Topic;

public class DirectCompiledSchemaRepository<T> implements CompiledSchemaRepository<T> {

  private final RawSchemaClient rawSchemaClient;
  private final SchemaCompiler<T> schemaCompiler;

  public DirectCompiledSchemaRepository(
      RawSchemaClient rawSchemaClient, SchemaCompiler<T> schemaCompiler) {
    this.rawSchemaClient = rawSchemaClient;
    this.schemaCompiler = schemaCompiler;
  }

  @Override
  public CompiledSchema<T> getSchema(Topic topic, SchemaVersion version, boolean online) {
    return rawSchemaClient
        .getRawSchemaWithMetadata(topic.getName(), version)
        .map(rawSchemaWithMetadata -> CompiledSchema.of(schemaCompiler, rawSchemaWithMetadata))
        .orElseThrow(() -> new SchemaNotFoundException(topic, version));
  }

  @Override
  public CompiledSchema<T> getSchema(Topic topic, SchemaId id) {
    return rawSchemaClient
        .getRawSchemaWithMetadata(topic.getName(), id)
        .map(rawSchemaWithMetadata -> CompiledSchema.of(schemaCompiler, rawSchemaWithMetadata))
        .orElseThrow(() -> new SchemaNotFoundException(id));
  }
}
