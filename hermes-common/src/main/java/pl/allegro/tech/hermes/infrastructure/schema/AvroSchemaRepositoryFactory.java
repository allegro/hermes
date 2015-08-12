package pl.allegro.tech.hermes.infrastructure.schema;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;

public class AvroSchemaRepositoryFactory extends AbstractSchemaRepositoryFactory<Schema> {

    private final ConfigFactory configFactory;
    private final SchemaSourceProvider schemaSourceProvider;

    @Inject
    public AvroSchemaRepositoryFactory(ConfigFactory configFactory, SchemaSourceProvider schemaSourceProvider) {
        this.configFactory = configFactory;
        this.schemaSourceProvider = schemaSourceProvider;
    }

    @Override
    public SchemaRepository<Schema> provide() {
        return new SchemaRepository<>(configFactory, schemaSourceProvider,
                createSchemaReloadExecutorService(configFactory, "avro"), source -> new Schema.Parser().parse(source.value()));
    }

    @Override
    public void dispose(SchemaRepository<Schema> instance) {

    }

}
