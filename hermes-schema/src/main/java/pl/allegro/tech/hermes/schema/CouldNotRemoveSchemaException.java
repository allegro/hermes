package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;

public class CouldNotRemoveSchemaException extends SchemaException {

    public CouldNotRemoveSchemaException(String message) {
        super(message);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_COULD_NOT_BE_DELETED;
    }
}
