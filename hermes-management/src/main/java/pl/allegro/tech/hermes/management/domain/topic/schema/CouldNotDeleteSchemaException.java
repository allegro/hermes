package pl.allegro.tech.hermes.management.domain.topic.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class CouldNotDeleteSchemaException extends HermesException {

    public CouldNotDeleteSchemaException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_COULD_NOT_BE_DELETED;
    }
}
