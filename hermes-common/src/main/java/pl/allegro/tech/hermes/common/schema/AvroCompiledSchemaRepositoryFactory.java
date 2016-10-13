package pl.allegro.tech.hermes.common.schema;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.schema.CachedCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaCompilersFactory;
import pl.allegro.tech.hermes.schema.RawSchemaClient;

import javax.inject.Inject;

public class AvroCompiledSchemaRepositoryFactory implements Factory<CompiledSchemaRepository<Schema>> {

    private final RawSchemaClient rawSchemaClient;
    private final ConfigFactory configFactory;

    @Inject
    public AvroCompiledSchemaRepositoryFactory(RawSchemaClient rawSchemaClient, ConfigFactory configFactory) {
        this.rawSchemaClient = rawSchemaClient;
        this.configFactory = configFactory;
    }

    @Override
    public CompiledSchemaRepository<Schema> provide() {
        return new CachedCompiledSchemaRepository<>(
                new DirectCompiledSchemaRepository<>(rawSchemaClient, SchemaCompilersFactory.avroSchemaCompiler()),
                configFactory.getIntProperty(Configs.SCHEMA_CACHE_COMPILED_MAXIMUM_SIZE),
                configFactory.getIntProperty(Configs.SCHEMA_CACHE_COMPILED_EXPIRE_AFTER_ACCESS_MINUTES));
    }

    @Override
    public void dispose(CompiledSchemaRepository<Schema> instance) {

    }
}
