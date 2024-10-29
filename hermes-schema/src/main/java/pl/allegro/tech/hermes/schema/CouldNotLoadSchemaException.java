package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;

public class CouldNotLoadSchemaException extends SchemaException {

  CouldNotLoadSchemaException(Throwable cause) {
    super(cause);
  }

  CouldNotLoadSchemaException(Topic topic, SchemaVersion version, Throwable cause) {
    super(
        String.format(
            "Schema for topic %s at version %d could not be loaded",
            topic.getQualifiedName(), version.value()),
        cause);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
  }
}
