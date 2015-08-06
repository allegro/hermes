package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class CouldNotCompileSchemaException extends HermesException {

    public CouldNotCompileSchemaException(Throwable cause) {
        super(cause);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.OTHER;
    }

}
