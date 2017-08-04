package pl.allegro.tech.hermes.schema;


import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;

public class NoSchemaVersionsFoundException extends SchemaException {

    NoSchemaVersionsFoundException(String message) {
        super(message);
    }

    NoSchemaVersionsFoundException(Topic topic) {
        this(String.format("No schema version found for topic %s", topic.getQualifiedName()));
    }


    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
    }
}
