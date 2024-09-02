package pl.allegro.tech.hermes.management.infrastructure.kafka;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class MovingSubscriptionOffsetsValidationException extends ManagementException {
  public MovingSubscriptionOffsetsValidationException(String msg) {
    super(msg);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.MOVING_SUBSCRIPTION_OFFSETS_VALIDATION_ERROR;
  }
}
