package pl.allegro.tech.hermes.infrastructure.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.domain.topic.schema.CachedConcreteSchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.ConcreteSchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.CouldNotCompileSchemaException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaSourceProvider;

import javax.inject.Inject;
import java.io.IOException;

public class JsonConcreteSchemaRepositoryFactory implements Factory<ConcreteSchemaRepository<JsonSchema>> {

    private final SchemaSourceProvider schemaSourceProvider;
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory jsonSchemaFactory;

    @Inject
    public JsonConcreteSchemaRepositoryFactory(ObjectMapper objectMapper, SchemaSourceProvider schemaSourceProvider) {
        this.objectMapper = objectMapper;
        this.schemaSourceProvider = schemaSourceProvider;
        this.jsonSchemaFactory = JsonSchemaFactory.byDefault();

    }

    @Override
    public ConcreteSchemaRepository<JsonSchema> provide() {
        // TODO extract property for max size
        return new CachedConcreteSchemaRepository<>(schemaSourceProvider, 2000,
                source -> {
                    try {
                        return jsonSchemaFactory.getJsonSchema(objectMapper.readTree(source.value()));
                    } catch (IOException | ProcessingException e) {
                        throw new CouldNotCompileSchemaException(e);
                    }
                });
    }

    @Override
    public void dispose(ConcreteSchemaRepository<JsonSchema> instance) {

    }
}
