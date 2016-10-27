package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;

public abstract class SchemaException extends RuntimeException {

    SchemaException(String message) {
        super(message);
    }

    SchemaException(Throwable cause) {
        super(cause);
    }

    SchemaException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract ErrorCode getCode();
}