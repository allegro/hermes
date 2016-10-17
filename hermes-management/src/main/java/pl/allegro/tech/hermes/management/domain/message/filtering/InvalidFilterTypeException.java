package pl.allegro.tech.hermes.management.domain.message.filtering;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class InvalidFilterTypeException extends HermesException {
    public InvalidFilterTypeException(String message) {
        super(message);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.INVALID_FILTER_TYPE_EXCEPTION;
    }
}
