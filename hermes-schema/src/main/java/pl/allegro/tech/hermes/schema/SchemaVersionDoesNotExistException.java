package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;

import static java.lang.String.format;

public class SchemaVersionDoesNotExistException extends SchemaException {

    SchemaVersionDoesNotExistException(Topic topic, SchemaVersion schemaVersion) {
        super(format("Schema version %s for topic %s does not exist", schemaVersion.value(), topic.getQualifiedName()));
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.OTHER;
    }
}
