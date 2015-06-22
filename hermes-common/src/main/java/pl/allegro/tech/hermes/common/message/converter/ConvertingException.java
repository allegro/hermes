package pl.allegro.tech.hermes.common.message.converter;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class ConvertingException extends InternalProcessingException {
    public ConvertingException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
