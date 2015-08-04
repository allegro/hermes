package pl.allegro.tech.hermes.common.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.glassfish.hk2.api.Factory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Executors;

public class JsonMessageSchemaRepositoryFactory implements Factory<MessageSchemaRepository<JsonSchema>> {

    private final ObjectMapper objectMapper;
    private final MessageSchemaSourceProvider messageSchemaSourceProvider;
    private JsonSchemaFactory jsonSchemaFactory;

    @Inject
    public JsonMessageSchemaRepositoryFactory(ObjectMapper objectMapper, MessageSchemaSourceProvider messageSchemaSourceProvider) {
        this.objectMapper = objectMapper;
        this.messageSchemaSourceProvider = messageSchemaSourceProvider;
    }

    @Override
    public MessageSchemaRepository<JsonSchema> provide() {
        jsonSchemaFactory = JsonSchemaFactory.byDefault();
        return new MessageSchemaRepository<>(messageSchemaSourceProvider, Executors.newFixedThreadPool(2),
                source -> {
                    try {
                        return jsonSchemaFactory.getJsonSchema(objectMapper.readTree(source));
                    } catch (IOException | ProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public void dispose(MessageSchemaRepository<JsonSchema> instance) {

    }
}
