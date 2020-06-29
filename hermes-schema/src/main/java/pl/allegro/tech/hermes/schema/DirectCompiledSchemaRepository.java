package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.Topic;

public class DirectCompiledSchemaRepository<T> implements CompiledSchemaRepository<T> {

    private final RawSchemaClient rawSchemaClient;
    private final SchemaCompiler<T> schemaCompiler;

    public DirectCompiledSchemaRepository(RawSchemaClient rawSchemaClient,
                                          SchemaCompiler<T> schemaCompiler) {
        this.rawSchemaClient = rawSchemaClient;
        this.schemaCompiler = schemaCompiler;
    }

    @Override
    public CompiledSchema<T> getSchema(Topic topic, SchemaVersion version, boolean online) {
        return rawSchemaClient.getSchemaWithId(topic.getName(), version)
                .map(schemaWithId -> CompiledSchema.of(schemaCompiler.compile(schemaWithId), schemaWithId.getId(), version.value()))
                .orElseThrow(() -> new SchemaNotFoundException(topic, version));
    }

    @Override
    public CompiledSchema<T> getSchema(SchemaId id, boolean online) {
        return null;
    }
}
