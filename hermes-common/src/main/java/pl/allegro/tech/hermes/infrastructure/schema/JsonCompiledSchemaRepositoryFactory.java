package pl.allegro.tech.hermes.infrastructure.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.main.JsonSchema;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.topic.schema.*;

import javax.inject.Inject;

public class JsonCompiledSchemaRepositoryFactory implements Factory<CompiledSchemaRepository<JsonSchema>> {

    private final SchemaSourceProvider schemaSourceProvider;
    private final ConfigFactory configFactory;
    private final ObjectMapper objectMapper;

    @Inject
    public JsonCompiledSchemaRepositoryFactory(ObjectMapper objectMapper, SchemaSourceProvider schemaSourceProvider, ConfigFactory configFactory) {
        this.objectMapper = objectMapper;
        this.schemaSourceProvider = schemaSourceProvider;
        this.configFactory = configFactory;
    }

    @Override
    public CompiledSchemaRepository<JsonSchema> provide() {
        return new CachedCompiledSchemaRepository<>(
                new DirectCompiledSchemaRepository<>(schemaSourceProvider, SchemaCompilersFactory.jsonSchemaCompiler(objectMapper)),
                configFactory.getIntProperty(Configs.SCHEMA_CACHE_COMPILED_MAXIMUM_SIZE));
    }

    @Override
    public void dispose(CompiledSchemaRepository<JsonSchema> instance) {

    }
}
