package pl.allegro.tech.hermes.infrastructure.schema;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE;

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
        return new SchemaRepository<>(
            Topic.ContentType.AVRO,
            schemaSourceProvider,
            createSchemaReloadExecutorService(configFactory.getIntProperty(SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE), "avro"),
            configFactory.getIntProperty(SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES),
            configFactory.getIntProperty(SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES),
            source -> new Schema.Parser().parse(source.value()));
    }

    @Override
    public void dispose(SchemaRepository<Schema> instance) {

    }

}
