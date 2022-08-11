package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

import static java.lang.String.format;

public class InvalidAvroIdlException extends ManagementException {

    InvalidAvroIdlException(Throwable cause) {
        super(format("Error while converting IDL Avro: %s", cause.getMessage()), cause);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_BAD_REQUEST;
    }
}
