package pl.allegro.tech.hermes.management.domain.subscription;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class UnhealthySubscriptionGetException extends ManagementException {

  public UnhealthySubscriptionGetException(String message) {
    super(message);
  }

  public UnhealthySubscriptionGetException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.OTHER;
  }
}
