package pl.allegro.tech.hermes.management.domain.message.filtering;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class FilterValidationException extends HermesException {
    public FilterValidationException(String message) {
        super(message);
    }

    public FilterValidationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.FILTER_VALIDATION_EXCEPTION;
    }
}
