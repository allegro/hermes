package pl.allegro.tech.hermes.infrastructure.schema;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.topic.schema.*;

import javax.inject.Inject;

public class AvroCompiledSchemaRepositoryFactory implements Factory<CompiledSchemaRepository<Schema>> {

    private final SchemaSourceProvider schemaSourceProvider;
    private final ConfigFactory configFactory;

    @Inject
    public AvroCompiledSchemaRepositoryFactory(SchemaSourceProvider schemaSourceProvider, ConfigFactory configFactory) {
        this.schemaSourceProvider = schemaSourceProvider;
        this.configFactory = configFactory;
    }

    @Override
    public CompiledSchemaRepository<Schema> provide() {
        return new CachedCompiledSchemaRepository<>(
                new DirectCompiledSchemaRepository<>(schemaSourceProvider, SchemaCompilersFactory.avroSchemaCompiler()),
                configFactory.getIntProperty(Configs.SCHEMA_CACHE_COMPILED_MAXIMUM_SIZE));
    }

    @Override
    public void dispose(CompiledSchemaRepository<Schema> instance) {

    }
}
