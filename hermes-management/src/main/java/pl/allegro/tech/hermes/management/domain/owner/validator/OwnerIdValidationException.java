package pl.allegro.tech.hermes.management.domain.owner.validator;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class OwnerIdValidationException extends ManagementException {

  public OwnerIdValidationException(String message) {
    super(message);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.VALIDATION_ERROR;
  }
}
