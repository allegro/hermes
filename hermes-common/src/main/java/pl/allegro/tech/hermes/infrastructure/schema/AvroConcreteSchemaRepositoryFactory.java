package pl.allegro.tech.hermes.infrastructure.schema;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.domain.topic.schema.CachedConcreteSchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.ConcreteSchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaSourceProvider;

import javax.inject.Inject;

public class AvroConcreteSchemaRepositoryFactory implements Factory<ConcreteSchemaRepository<Schema>> {

    private final SchemaSourceProvider schemaSourceProvider;

    @Inject
    public AvroConcreteSchemaRepositoryFactory(SchemaSourceProvider schemaSourceProvider) {
        this.schemaSourceProvider = schemaSourceProvider;
    }

    @Override
    public ConcreteSchemaRepository<Schema> provide() {
        // TODO extract property for max size
        return new CachedConcreteSchemaRepository<>(schemaSourceProvider, 2000,
                source -> new Schema.Parser().parse(source.value()));
    }

    @Override
    public void dispose(ConcreteSchemaRepository<Schema> instance) {

    }
}
