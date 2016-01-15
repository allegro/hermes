package pl.allegro.tech.hermes.infrastructure.schema;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.domain.topic.schema.CachedSchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;

public class AvroSchemaRepositoryFactory implements Factory<SchemaRepository<Schema>> {

    private final CachedSchemaSourceProvider cachedSchemaSourceProvider;

    @Inject
    public AvroSchemaRepositoryFactory(CachedSchemaSourceProvider cachedSchemaSourceProvider) {
        this.cachedSchemaSourceProvider = cachedSchemaSourceProvider;
    }

    @Override
    public SchemaRepository<Schema> provide() {
        return new SchemaRepository<>(
            ContentType.AVRO,
            cachedSchemaSourceProvider,
            source -> new Schema.Parser().parse(source.value()));
    }

    @Override
    public void dispose(SchemaRepository<Schema> instance) {

    }

}
