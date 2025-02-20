package pl.allegro.tech.hermes.management.domain.subscription.validator;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class SubscriptionValidationException extends ManagementException {

  public SubscriptionValidationException(String message) {
    super(message);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.VALIDATION_ERROR;
  }
}
