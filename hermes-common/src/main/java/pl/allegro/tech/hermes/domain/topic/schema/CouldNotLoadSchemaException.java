package pl.allegro.tech.hermes.domain.topic.schema;

public class CouldNotLoadSchemaException extends RuntimeException {

    public CouldNotLoadSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}
