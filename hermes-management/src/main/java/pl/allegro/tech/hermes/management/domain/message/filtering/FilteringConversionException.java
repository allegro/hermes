package pl.allegro.tech.hermes.management.domain.message.filtering;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class FilteringConversionException extends HermesException {
    public FilteringConversionException(String message) {
        super(message);
    }

    @Override
        public ErrorCode getCode() {
            return ErrorCode.FORMAT_ERROR;
        }
}
