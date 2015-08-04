package pl.allegro.tech.hermes.domain.topic.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.glassfish.hk2.api.Factory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Executors;

public class JsonSchemaRepositoryFactory implements Factory<SchemaRepository<JsonSchema>> {

    private final ObjectMapper objectMapper;
    private final SchemaSourceProvider schemaSourceProvider;
    private JsonSchemaFactory jsonSchemaFactory;

    @Inject
    public JsonSchemaRepositoryFactory(ObjectMapper objectMapper, SchemaSourceProvider schemaSourceProvider) {
        this.objectMapper = objectMapper;
        this.schemaSourceProvider = schemaSourceProvider;
    }

    @Override
    public SchemaRepository<JsonSchema> provide() {
        jsonSchemaFactory = JsonSchemaFactory.byDefault();
        return new SchemaRepository<>(schemaSourceProvider, Executors.newFixedThreadPool(2),
                source -> {
                    try {
                        return jsonSchemaFactory.getJsonSchema(objectMapper.readTree(source.value()));
                    } catch (IOException | ProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public void dispose(SchemaRepository<JsonSchema> instance) {

    }
}
