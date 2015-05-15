package pl.allegro.tech.hermes.common.exception;

import pl.allegro.tech.hermes.api.ErrorCode;


public abstract class HermesException extends RuntimeException {
    public HermesException(Throwable t) {
        super(t);
    }

    public HermesException(String message) {
        super(message);
    }

    public HermesException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract ErrorCode getCode();
}
