package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import static java.lang.String.format;

public class DirectCompiledSchemaRepository<T> implements CompiledSchemaRepository<T> {

    private final SchemaSourceProvider schemaSourceProvider;
    private final SchemaCompiler<T> schemaCompiler;

    public DirectCompiledSchemaRepository(SchemaSourceProvider schemaSourceProvider,
                                          SchemaCompiler<T> schemaCompiler) {
        this.schemaSourceProvider = schemaSourceProvider;
        this.schemaCompiler = schemaCompiler;
    }

    @Override
    public CompiledSchema<T> getSchema(Topic topic, SchemaVersion version) {
        try {
            SchemaSource schemaSource = schemaSourceProvider.get(topic, version)
                    .orElseThrow(() -> new SchemaSourceNotFoundException(topic, version));
            return new CompiledSchema<>(schemaCompiler.compile(schemaSource), version);
        } catch (Exception e) {
            throw new CouldNotLoadSchemaException(
                    format("Could not load schema type of %s for topic %s",
                            topic.getContentType(), topic.getQualifiedName()), e);
        }
    }

}
