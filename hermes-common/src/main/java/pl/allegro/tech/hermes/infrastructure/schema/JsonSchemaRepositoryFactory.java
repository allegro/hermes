package pl.allegro.tech.hermes.infrastructure.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.domain.topic.schema.CachedSchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.CouldNotCompileSchemaException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;
import java.io.IOException;

public class JsonSchemaRepositoryFactory implements Factory<SchemaRepository<JsonSchema>> {

    private final ObjectMapper objectMapper;
    private final CachedSchemaSourceProvider cachedSchemaSourceProvider;
    private final JsonSchemaFactory jsonSchemaFactory;

    @Inject
    public JsonSchemaRepositoryFactory(ObjectMapper objectMapper, CachedSchemaSourceProvider cachedSchemaSourceProvider) {
        this.objectMapper = objectMapper;
        this.cachedSchemaSourceProvider = cachedSchemaSourceProvider;
        this.jsonSchemaFactory = JsonSchemaFactory.byDefault();
    }

    @Override
    public SchemaRepository<JsonSchema> provide() {
        return new SchemaRepository<>(
                ContentType.JSON,
                cachedSchemaSourceProvider,
                source -> {
                    try {
                        return jsonSchemaFactory.getJsonSchema(objectMapper.readTree(source.value()));
                    } catch (IOException | ProcessingException e) {
                        throw new CouldNotCompileSchemaException(e);
                    }
                });
    }

    @Override
    public void dispose(SchemaRepository<JsonSchema> instance) {

    }
}
