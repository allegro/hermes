package pl.allegro.tech.hermes.common.message.serialization;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class DeserializationException extends InternalProcessingException {
    public DeserializationException(String message, Exception cause) {
        super(message, cause);
    }

    public DeserializationException(String message) {
        super(message);
    }
}
