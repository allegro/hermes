package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.RawSchema;
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
    public CompiledSchema<T> getSchema(Topic topic, SchemaVersion version) {
        try {
            RawSchema rawSchema = rawSchemaClient.getSchema(topic.getName(), version)
                    .orElseThrow(() -> new SchemaNotFoundException(topic, version));
            return new CompiledSchema<>(schemaCompiler.compile(rawSchema), version);
        } catch (Exception e) {
            throw new CouldNotLoadSchemaException(
                    String.format("Could not load schema type of %s for topic %s",
                            topic.getContentType(), topic.getQualifiedName()), e);
        }
    }

}
