package pl.allegro.tech.hermes.management.domain.endpoint;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class EndpointValidationException extends ManagementException {

    public EndpointValidationException(String message) {
        super(message);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.VALIDATION_ERROR;
    }
}
