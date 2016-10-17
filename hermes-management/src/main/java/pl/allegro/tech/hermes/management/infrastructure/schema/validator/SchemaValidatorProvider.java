package pl.allegro.tech.hermes.management.infrastructure.schema.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ContentType;

import java.util.EnumMap;
import java.util.Map;

@Component
public class SchemaValidatorProvider {

    private final Map<ContentType, SchemaValidator> validators = new EnumMap<>(ContentType.class);

    @Autowired
    public SchemaValidatorProvider(AvroSchemaValidator avroSchemaValidator) {
        validators.put(ContentType.AVRO, avroSchemaValidator);
    }

    public SchemaValidator provide(ContentType contentType) {
        return validators.get(contentType);
    }


}
