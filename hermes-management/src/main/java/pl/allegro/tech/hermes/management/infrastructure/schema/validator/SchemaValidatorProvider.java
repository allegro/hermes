package pl.allegro.tech.hermes.management.infrastructure.schema.validator;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

@Component
public class SchemaValidatorProvider {

  private final Map<ContentType, SchemaValidator> validators = new EnumMap<>(ContentType.class);

  @Autowired
  public SchemaValidatorProvider(AvroSchemaValidator avroSchemaValidator) {
    validators.put(ContentType.AVRO, avroSchemaValidator);
  }

  public SchemaValidator provide(ContentType contentType) {
    if (!validators.containsKey(contentType)) {
      throw new SchemaValidatorNotAvailable(
          "No schema validator for content-type: " + contentType.name());
    }

    return validators.get(contentType);
  }

  class SchemaValidatorNotAvailable extends ManagementException {
    public SchemaValidatorNotAvailable(String message) {
      super(message);
    }

    @Override
    public ErrorCode getCode() {
      return ErrorCode.VALIDATION_ERROR;
    }
  }
}
