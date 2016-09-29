package pl.allegro.tech.hermes.management.infrastructure.schema.validator;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class InvalidSchemaException extends HermesException {

    InvalidSchemaException(Throwable cause) {
        super("Error while trying to validate schema", cause);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.FORMAT_ERROR;
    }
}
