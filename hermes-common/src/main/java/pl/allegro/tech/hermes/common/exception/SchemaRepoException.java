package pl.allegro.tech.hermes.common.exception;

import pl.allegro.tech.hermes.api.ErrorCode;

public class SchemaRepoException extends HermesException {

    public SchemaRepoException(String message) {
        super(message);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.INTERNAL_ERROR;
    }

}
