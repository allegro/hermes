package pl.allegro.tech.hermes.management.infrastructure.schema.validator;

import static java.lang.String.format;

import org.apache.avro.SchemaParseException;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class InvalidSchemaException extends HermesException {

  InvalidSchemaException(String cause) {
    super(format("Error while trying to validate schema: %s", cause));
  }

  InvalidSchemaException(SchemaParseException cause) {
    super(format("Error while trying to validate schema: %s", cause.getMessage()), cause);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.FORMAT_ERROR;
  }
}
