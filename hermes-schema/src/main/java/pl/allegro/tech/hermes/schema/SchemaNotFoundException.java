package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;

public class SchemaNotFoundException extends SchemaException {

  SchemaNotFoundException(Topic topic, SchemaVersion schemaVersion) {
    super(
        "No schema source for topic "
            + topic.getQualifiedName()
            + " at version "
            + schemaVersion.value());
  }

  SchemaNotFoundException(Topic topic) {
    super("No schema source for topic " + topic.getQualifiedName());
  }

  SchemaNotFoundException(SchemaId id) {
    super("No schema source for id " + id.value());
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.OTHER;
  }
}
