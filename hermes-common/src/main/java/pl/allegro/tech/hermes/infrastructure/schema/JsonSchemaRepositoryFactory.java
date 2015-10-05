package pl.allegro.tech.hermes.infrastructure.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.exception.HermesException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;
import java.io.IOException;

import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE;

public class JsonSchemaRepositoryFactory extends AbstractSchemaRepositoryFactory<JsonSchema> {

    private final ConfigFactory configFactory;
    private final ObjectMapper objectMapper;
    private final SchemaSourceProvider schemaSourceProvider;
    private final JsonSchemaFactory jsonSchemaFactory;

    @Inject
    public JsonSchemaRepositoryFactory(ConfigFactory configFactory, ObjectMapper objectMapper, SchemaSourceProvider schemaSourceProvider) {
        this.configFactory = configFactory;
        this.objectMapper = objectMapper;
        this.schemaSourceProvider = schemaSourceProvider;
        this.jsonSchemaFactory = JsonSchemaFactory.byDefault();
    }

    @Override
    public SchemaRepository<JsonSchema> provide() {
        return new SchemaRepository<>(
                Topic.ContentType.JSON,
                schemaSourceProvider,
                createSchemaReloadExecutorService(configFactory.getIntProperty(SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE), "json"),
                configFactory.getIntProperty(SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES),
                configFactory.getIntProperty(SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES),
                source -> {
                    try {
                        return jsonSchemaFactory.getJsonSchema(objectMapper.readTree(source.value()));
                    } catch (IOException | ProcessingException e) {
                        throw new JsonSchemaCompilationFailed(e);
                    }
                });
    }

    @Override
    public void dispose(SchemaRepository<JsonSchema> instance) {

    }

    private static class JsonSchemaCompilationFailed extends HermesException {

        public JsonSchemaCompilationFailed(Exception e) {
            super(e);
        }

        @Override
        public ErrorCode getCode() {
            return ErrorCode.OTHER;
        }

    }
}
