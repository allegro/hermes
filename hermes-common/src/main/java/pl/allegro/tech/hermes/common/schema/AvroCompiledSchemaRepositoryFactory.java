package pl.allegro.tech.hermes.common.schema;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.schema.CachedCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaCompilersFactory;
import pl.allegro.tech.hermes.schema.SchemaSourceClient;

import javax.inject.Inject;

public class AvroCompiledSchemaRepositoryFactory implements Factory<CompiledSchemaRepository<Schema>> {

    private final SchemaSourceClient schemaSourceClient;
    private final ConfigFactory configFactory;

    @Inject
    public AvroCompiledSchemaRepositoryFactory(SchemaSourceClient schemaSourceClient, ConfigFactory configFactory) {
        this.schemaSourceClient = schemaSourceClient;
        this.configFactory = configFactory;
    }

    @Override
    public CompiledSchemaRepository<Schema> provide() {
        return new CachedCompiledSchemaRepository<>(
                new DirectCompiledSchemaRepository<>(schemaSourceClient, SchemaCompilersFactory.avroSchemaCompiler()),
                configFactory.getIntProperty(Configs.SCHEMA_CACHE_COMPILED_MAXIMUM_SIZE),
                configFactory.getIntProperty(Configs.SCHEMA_CACHE_COMPILED_EXPIRE_AFTER_ACCESS_MINUTES));
    }

    @Override
    public void dispose(CompiledSchemaRepository<Schema> instance) {

    }
}
