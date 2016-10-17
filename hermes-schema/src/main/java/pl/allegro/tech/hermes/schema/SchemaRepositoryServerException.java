package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;

public class SchemaRepositoryServerException extends SchemaException {

    public SchemaRepositoryServerException(String message) {
        super(message);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.INTERNAL_ERROR;
    }
}
