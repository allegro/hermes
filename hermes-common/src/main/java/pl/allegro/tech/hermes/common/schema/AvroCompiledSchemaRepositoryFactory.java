package pl.allegro.tech.hermes.common.schema;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.schema.CachedCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaCompilersFactory;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_COMPILED_MAXIMUM_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_ENABLED;

public class AvroCompiledSchemaRepositoryFactory implements Factory<CompiledSchemaRepository<Schema>> {

    private final RawSchemaClient rawSchemaClient;
    private final ConfigFactory configFactory;

    @Inject
    public AvroCompiledSchemaRepositoryFactory(RawSchemaClient rawSchemaClient,
                                               ConfigFactory configFactory) {
        this.rawSchemaClient = rawSchemaClient;
        this.configFactory = configFactory;
    }

    @Override
    public CompiledSchemaRepository<Schema> provide() {
        CompiledSchemaRepository<Schema> repository = new DirectCompiledSchemaRepository<>(rawSchemaClient,
                SchemaCompilersFactory.avroSchemaCompiler());

        if (configFactory.getBooleanProperty(SCHEMA_CACHE_ENABLED)) {
            return new CachedCompiledSchemaRepository<>(repository,
                    configFactory.getIntProperty(SCHEMA_CACHE_COMPILED_MAXIMUM_SIZE),
                    configFactory.getIntProperty(Configs.SCHEMA_CACHE_COMPILED_EXPIRE_AFTER_ACCESS_MINUTES));
        } else {
            return repository;
        }
    }

    @Override
    public void dispose(CompiledSchemaRepository<Schema> instance) {

    }
}
