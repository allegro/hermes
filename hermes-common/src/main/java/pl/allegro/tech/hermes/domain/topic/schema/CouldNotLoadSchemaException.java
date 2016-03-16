package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class CouldNotLoadSchemaException extends HermesException {

    public CouldNotLoadSchemaException(Throwable cause) {
        super(cause);
    }

    public CouldNotLoadSchemaException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
    }
}
