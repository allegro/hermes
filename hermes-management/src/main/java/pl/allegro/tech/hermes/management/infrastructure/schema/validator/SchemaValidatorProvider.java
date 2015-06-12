package pl.allegro.tech.hermes.management.infrastructure.schema.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Topic;

import java.util.HashMap;
import java.util.Map;

@Component
public class SchemaValidatorProvider {

    private final Map<Topic.ContentType, SchemaValidator> validators = new HashMap<>(2);
    private final JsonSchemaValidator jsonSchemaValidator;

    @Autowired
    public SchemaValidatorProvider(JsonSchemaValidator jsonSchemaValidator, AvroSchemaValidator avroSchemaValidator) {
        this.jsonSchemaValidator = jsonSchemaValidator;

        validators.put(Topic.ContentType.AVRO, avroSchemaValidator);
        validators.put(Topic.ContentType.JSON, jsonSchemaValidator);
    }

    public SchemaValidator provide(Topic.ContentType contentType) {
        return validators.getOrDefault(contentType, jsonSchemaValidator);
    }


}
