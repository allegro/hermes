package pl.allegro.tech.hermes.management.domain.maintainer.validator;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class MaintainerDescriptorValidationException extends ManagementException {

    public MaintainerDescriptorValidationException(String message) {
        super(message);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.VALIDATION_ERROR;
    }

}
