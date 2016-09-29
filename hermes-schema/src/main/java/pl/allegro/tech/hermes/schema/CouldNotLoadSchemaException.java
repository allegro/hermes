package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;

public class CouldNotLoadSchemaException extends SchemaException {

    CouldNotLoadSchemaException(Throwable cause) {
        super(cause);
    }

    CouldNotLoadSchemaException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
    }
}
