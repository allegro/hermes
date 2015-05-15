package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class SingleMessageReaderException extends ManagementException {

    public SingleMessageReaderException(String message) {
        super(message);
    }

    public SingleMessageReaderException(String message, Throwable t) {
        super(message, t);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SINGLE_MESSAGE_READER_EXCEPTION;
    }
}
