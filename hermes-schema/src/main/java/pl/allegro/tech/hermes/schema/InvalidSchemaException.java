package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;

public class InvalidSchemaException extends SchemaException {

    public InvalidSchemaException(String message) {
        super(message);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.FORMAT_ERROR;
    }
}
