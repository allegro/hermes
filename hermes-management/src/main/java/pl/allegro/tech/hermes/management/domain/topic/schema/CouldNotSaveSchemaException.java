package pl.allegro.tech.hermes.management.domain.topic.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class CouldNotSaveSchemaException extends HermesException {

    public CouldNotSaveSchemaException(String message, Throwable t) {
        super(message, t);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_COULD_NOT_BE_SAVED;
    }
}
