package pl.allegro.tech.hermes.schema;

import static java.lang.String.format;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;

public class SchemaVersionDoesNotExistException extends SchemaException {

  private final SchemaVersion schemaVersion;

  SchemaVersionDoesNotExistException(Topic topic, SchemaVersion schemaVersion) {
    super(
        format(
            "Schema version %s for topic %s does not exist",
            schemaVersion.value(), topic.getQualifiedName()));
    this.schemaVersion = schemaVersion;
  }

  public SchemaVersion getSchemaVersion() {
    return schemaVersion;
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.OTHER;
  }
}
